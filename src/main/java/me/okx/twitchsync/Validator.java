package me.okx.twitchsync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import me.okx.twitchsync.data.*;
import me.okx.twitchsync.data.json.AccessToken;
import me.okx.twitchsync.data.json.ChannelObject;
import me.okx.twitchsync.data.json.CheckError;
import me.okx.twitchsync.data.json.User;
import me.okx.twitchsync.data.json.Users;
import me.okx.twitchsync.data.sync.SyncMessage;
import me.okx.twitchsync.data.sync.SyncResponse;
import me.okx.twitchsync.events.PlayerSubscriptionEvent;
import me.okx.twitchsync.util.SqlHelper;
import me.okx.twitchsync.util.WebUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Validator {
  public static final String SUBSCRIPTIONS = "subscriptions";
  public static final String FOLLOWS = "follows/channels";
  private Gson gson = new Gson();
  private Map<Integer, Channel> channels;
  private TwitchSync plugin;
  private Cache<UUID, UUID> userStates;

  public Validator(TwitchSync plugin) {
    this.userStates = CacheBuilder.newBuilder()
        .expireAfterWrite(plugin.getConfig().getInt("expiry-time"), TimeUnit.MINUTES)
        .build();
    this.plugin = plugin;
    CompletableFuture.runAsync(() -> {
      plugin.debug("Getting Channels.", "Testing");
      List<Channel> channelList = (List<Channel>)plugin.getConfig().getList("channels");
      plugin.debug(channelList, "Channels");
      if (channelList == null) channelList = new ArrayList<>();
      this.channels = getChannelMap(channelList);
      plugin.debug("Checking channels: " + channelList.stream().map(Channel::getName).collect(Collectors.joining(",")), "Channels");
      plugin.debug("Got channels: " + this.channels.values().stream().map(Channel::getName).collect(Collectors.joining(",")), "Channels");
      if (channelList.size() == 0) {
        plugin.debug("Got an empty channel list, writing an example to file.", "Channels");
        Channel example = new Channel();
        example.setName("example");
        Options exampleOptions = new Options();
        exampleOptions.setRank("none");
        exampleOptions.setEnabled(false);
        exampleOptions.setCommands(Collections.emptyList());
        exampleOptions.setRevokeCommands(Collections.emptyList());
        example.setSubscribe(exampleOptions);
        example.setFollow(exampleOptions);
        channelList.add(example);
        plugin.getConfig().set("channels", channelList);
        plugin.saveConfig();
      }
    });
  }

  private <T> List<T> getList(String path, Class<T> clazz) {
    List<?> _list = plugin.getConfig().getList(path);
    List<T> list = new ArrayList<>();
    if (_list == null) return list;
    plugin.debug(_list, "Channels");

    for (Object t : _list) {
      if (clazz.isInstance(t)) {
        list.add(clazz.cast(t));
      }
    }
    return list;
  }

  public Channel getChannel(int channelId) {
    return channels.get(channelId);
  }

  private Map<Integer, Channel> getChannelMap(List<Channel> channels) {
    try {
      Map<String, String> headers = new HashMap<>();

      headers.put("Client-ID", plugin.getConfig().getString("client-id"));
      headers.put("Accept", "application/vnd.twitchtv.v5+json");

      if (channels.size() == 0) {
        plugin.debug("No channels found", "Error");
        return new HashMap<>();
      }

      Reader reader = WebUtil.getURL("https://api.twitch.tv/kraken/users?login="
          + channels.stream().map(Channel::getName).collect(Collectors.joining(",")), headers);
      Users users = plugin.debug(gson.fromJson(reader, Users.class), "Users");

      if (users.getTotal() == 0) {
        plugin.debug("No channels found", "Error");
        return new HashMap<>();
      }

      Map<Integer, Channel> channelMap = new HashMap<>();
      for (User user : users.getUsers()) {
        channelMap.put(user.getId(), channels.stream().filter(c -> c.getName().equals(user.getName())).findFirst().get());
      }
      return channelMap;
    } catch (Exception ex) {
      plugin.debug(ex);
      return new HashMap<>();
    }
  }

  public String createAuthenticationUrl(UUID user) {
    UUID uuid = UUID.randomUUID();
    userStates.put(uuid, user);

    return "https://id.twitch.tv/oauth2/authorize" +
        "?response_type=code" +
        "&client_id=" + plugin.getConfig().getString("client-id") +
        "&redirect_uri=" + plugin.getConfig().getString("redirect-uri") +
        "&scope=user_subscriptions+openid" +
        "&state=" + uuid;
  }

  private String getUserId(AccessToken token) throws IOException, ParseException, BadJOSEException, JOSEException {

    Issuer iss = new Issuer("https://id.twitch.tv/oauth2");
    ClientID clientID = new ClientID(plugin.getConfig().getString("client-id"));
    JWSAlgorithm jwsAlg = JWSAlgorithm.RS256;
    URL jwkSetURL = new URL("https://id.twitch.tv/oauth2/keys");

    IDTokenValidator validator = new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
    JWT idToken = JWTParser.parse(token.getIdToken());

    IDTokenClaimsSet claims;

    claims = validator.validate(idToken, null);
    return claims.getSubject().getValue();
  }

  private AccessToken getAccessToken(String code) {
    Reader reader = WebUtil.getURL("https://id.twitch.tv/oauth2/token" +
            "?client_id=" + plugin.getConfig().getString("client-id") +
            "&client_secret=" + plugin.getConfig().getString("client-secret") +
            "&code=" + code +
            "&grant_type=authorization_code" +
            "&redirect_uri=" + plugin.getConfig().getString("redirect-uri"),
        new HashMap<>(), "POST");
    return gson.fromJson(reader, AccessToken.class);
  }


  private Stream<StateWithId> getStates(Token token, Map<Integer, Channel> channels, String type) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Client-ID", plugin.getConfig().getString("client-id"));
    headers.put("Accept", "application/vnd.twitchtv.v5+json");
    headers.put("Authorization", "OAuth " + token.getAccessToken().getAccessToken());

    return channels.entrySet().stream()
        .map(entry -> getIndividualState(entry.getValue(), headers,
            "https://api.twitch.tv/kraken/users/" + token.getId() + "/" + type + "/", entry.getKey()));
  }

  private StateWithId getIndividualState(Channel channel, Map<String, String> headers, String url, int channelId) {
    InputStreamReader reader = WebUtil.getURL(plugin.debug(url + channelId, "Sync"), headers);
    JsonElement json = gson.fromJson(reader, JsonElement.class);

    ChannelObject object = gson.fromJson(json, ChannelObject.class);
    if (object.isValid()) {
      return new StateWithId(CheckState.YES, channel, channelId);
    }

    // user is not subscribed
    CheckError error = gson.fromJson(json, CheckError.class);
    switch (error.getStatus()) {
      case 404:
        return new StateWithId(CheckState.NO, channel, channelId);
      case 422:
        return new StateWithId(CheckState.UNPROCESSABLE, channel, channelId);
      default:
        plugin.debug("Check state: " + error);
        return new StateWithId(CheckState.ERROR, channel, channelId);
    }
  }

  public AccessToken refreshToken(String refreshToken) {
    InputStreamReader reader = WebUtil.getURL("https://id.twitch.tv/oauth2/token" +
            "?grant_type=refresh_token" +
            "&refresh_token=" + refreshToken +
            "&client_id=" + plugin.getConfig().getString("client-id") +
            "&client_secret=" + plugin.getConfig().getString("client-secret"),
        new HashMap<>(), "POST");
    return gson.fromJson(reader, AccessToken.class);
  }

  public CompletableFuture<Token> store(UUID uuid, String code) {
    try {
      AccessToken token = getAccessToken(code);
      return plugin.getSqlHelper().setToken(uuid, getUserId(token), token);
    } catch (IOException | ParseException | BadJOSEException | JOSEException ex) {
      return CompletableFuture.completedFuture(null);
    }
  }

  public CompletableFuture<Void> sync(Map<UUID, Token> users) {
    return CompletableFuture.runAsync(() -> users.forEach(this::sync));
  }

  public CompletableFuture<SyncResponse> sync(UUID uuid) {
    return CompletableFuture.supplyAsync(() ->
        plugin.getSqlHelper().getToken(uuid).map(token ->
            sync(plugin.debug(uuid), plugin.debug(token)).join())
            .orElse(SyncResponse.of(SyncMessage.NO_TOKEN)));
  }

  public CompletableFuture<SyncResponse> sync(UUID uuid, Token token) {
    return CompletableFuture.<SyncResponse>supplyAsync(() -> {
      if (token == null) {
        return SyncResponse.of(SyncMessage.NO_TOKEN);
      }

      if (channels == null) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
          player.sendMessage(ChatColor.DARK_RED + "No channels available, contact your administrator.");
        }
        plugin.getLogger().info("No channels found to check for subscriptions.");
        return SyncResponse.of(SyncMessage.SUCCESS);
      }

      Token _token = refreshAndSaveToken(uuid, token);

      if (_token == null) {
        _token = plugin.getSqlHelper().getToken(uuid).get();
      }

      if (_token.getId() == null) {
        try {
          _token.setId(getUserId(_token.getAccessToken()));
        } catch (Exception ex) {
          plugin.debug(ex);
        }
      }
      plugin.debug("ID: " + _token.getId(), "Sync");

      Stream<StateWithId> subscribeStates = getStates(_token, channels, SUBSCRIPTIONS);
      List<Channel> subscriptions = syncStates(uuid, subscribeStates, Channel::getSubscribe, SqlHelper::setSubscribed);

      Stream<StateWithId> followStates = getStates(_token, channels, FOLLOWS);
      List<Channel> follows = syncStates(uuid, followStates, Channel::getFollow, SqlHelper::setFollowing);

      Integer subscriptionCount = subscriptions.size();
      // get highest upgrade
      // get upgrades
      List<Upgrade> upgrades = getList("upgrades", Upgrade.class);
      upgrades.sort(Comparator.comparingInt(Upgrade::getThreshold));
      OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
      Permission perms = plugin.getPerms();

      for (Upgrade u : upgrades) {
        if (u.getThreshold() <= subscriptions.size()) {
          if (!perms.playerInGroup(null, player, u.getRank())) {
            perms.playerAddGroup(null, player, u.getRank());
          }
        } else if (perms.playerInGroup(null, player, u.getRank())) {
          perms.playerRemoveGroup(null, player, u.getRank());
        }
      }

      // todo: check total subscription count for upgrades
      return new SyncResponse(SyncMessage.SUCCESS, subscriptions, follows);
    });
  }

  private List<Channel> syncStates(UUID uuid, Stream<StateWithId> states, OptionSupplier optionSupplier, Persistence persistence) {
    List<Channel> subscriptions = new ArrayList<>();
    SqlHelper helper = plugin.getSqlHelper();
    Permission perms = plugin.getPerms();
    states.forEach(state -> {
      Boolean active = null;
      if (state.getState() == CheckState.YES) active = true;
      else if (state.getState() == CheckState.NO) active = false;
      Channel channel = state.getChannel();
      plugin.debug(uuid.toString() + " - " + channel.getName() + ": " + state.getState().toString(), "Sync");
      if (active != null) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) { // player is online
          Options options = optionSupplier.supply(channel);
          Boolean wasActive = helper.isSubscribed(uuid, channel.getName());
          persistence.persist(helper, uuid, channel.getName(), active);
          if (!wasActive && active) {
            Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.getPluginManager().callEvent(
                    new PlayerSubscriptionEvent(player, channel)));
          } else if (wasActive && !active) {
            // remove manually until event is implemented
            if (perms.playerInGroup(null, player, options.getRank())) {
              perms.playerRemoveGroup(null, player, options.getRank());
            }

            for (String command : options.getRevokeCommands()) {
              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                  .replace("%player%", player.getName())
                  .replace("%channel%", channel.getName()));
            }

          }
        } else {
          plugin.debug("Got subscription or follow but player is offline, ignoring.", "Sync");
        }
      }
    });
    return subscriptions;
  }

  private Token refreshAndSaveToken(UUID uuid, Token token) {
    AccessToken newToken = refreshToken(token.getAccessToken().getRefreshToken());
    try {
      return plugin.getSqlHelper().setToken(uuid, token.getId(), newToken).get();
    } catch (Exception ex) {
      plugin.debug(ex);
      return null;
    }
  }

  public UUID getUUIDFromAuthState(UUID stateUUID) {
    return this.userStates.getIfPresent(stateUUID);
  }
}
