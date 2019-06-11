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
import me.okx.twitchsync.data.sync.SyncResponseFailure;
import me.okx.twitchsync.data.sync.SyncResponseSuccess;
import me.okx.twitchsync.events.PlayerFollowEvent;
import me.okx.twitchsync.events.PlayerSubscriptionEvent;
import me.okx.twitchsync.util.SqlHelper;
import me.okx.twitchsync.util.WebUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
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
      List<Channel> channelList = new ArrayList<>();
      ConfigurationSection channelConfig = plugin.getConfig().getConfigurationSection("channels");
      if (channelConfig != null) {
        Set<String> channelNames = channelConfig.getKeys(false);
        for (String name : channelNames) {
          channelList.add(channelConfig.getObject(name, Channel.class));
        }
      }
      this.channels = getChannelMap(channelList);
    });
  }

  public Channel getChannel(int channelId) {
    return channels.get(channelId);
  }

  private Map<Integer, Channel> getChannelMap(List<Channel> channels) {
    Map<String, String> headers = new HashMap<>();

    headers.put("Client-ID", plugin.getConfig().getString("client-id"));
    headers.put("Accept", "application/vnd.twitchtv.v5+json");

    Reader reader = WebUtil.getURL("https://api.twitch.tv/kraken/users?login="
        + channels.stream().map(Channel::getName).collect(Collectors.joining(",")), headers);
    Users users = plugin.debug(gson.fromJson(reader, Users.class), "Users");

    if (users.getTotal() == 0) {
      plugin.getLogger().log(Level.SEVERE, "No channels found");
      return null;
    }

    Map<Integer, Channel> channelMap = new HashMap<>();
    for (User user : users.getUsers()) {
      channelMap.put(user.getId(), channels.stream().filter(c -> c.getName().equals(user.getName())).findFirst().get());
    }
    return channelMap;
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

  /*
  public SyncResponse sync(UUID withState, String code) {
    plugin.debug("States: " + userStates.asMap());

    UUID uuid = userStates.getIfPresent(withState);
    if (uuid == null) {
      return new SyncResponseFailure(SyncMessage.STATE_NOT_FOUND);
    }
    userStates.invalidate(withState);

    // safe to run blocking sql as this is run on web server thread
    Optional<Boolean> following = plugin.getSqlHelper().isFollowing(uuid);
    Optional<Boolean> subscribed = plugin.getSqlHelper().isSubscribed(uuid);

    if (!subscribed.isPresent() || !following.isPresent()) {
      return new SyncResponseFailure(SyncMessage.UNKNOWN_ERROR);
    }

    Player user = Bukkit.getPlayer(uuid);

    if (user == null) {
      return new SyncResponseFailure(SyncMessage.PLAYER_NOT_FOUND);
    }

    AccessToken token;
    try {
      token = plugin.debug(getAccessToken(code));
    } catch (Exception ex) {
      plugin.debug(ex);
      return new SyncResponseFailure(SyncMessage.UNKNOWN_ERROR);
    }


    try {
      String userId = plugin.debug(getUserId(token), "User ID");
      plugin.getSqlHelper().setToken(uuid, userId, token.getAccessToken(), token.getRefreshToken());

      MessageWithId subscriptionMessage = getSubscriptionMessage(userId, token);
      if (subscriptionMessage.getMessage() == SyncMessage.SUBSCRIPTION_SUCCESS) {
        if (plugin.debug(subscribed.get(), "Subscribed")) {
          subscriptionMessage.setMessage(SyncMessage.ALREADY_DONE);
        } else {
          Bukkit.getScheduler().runTask(plugin, () ->
              Bukkit.getPluginManager().callEvent(
                  new PlayerSubscriptionEvent(user, subscriptionMessage.getChannelId().get())));
          plugin.getSqlHelper().setSubscribed(uuid, true);
        }
      }

      MessageWithId followingMessage = getFollowingMessage(userId, token);
      if (followingMessage.getMessage() == SyncMessage.FOLLOW_SUCCESS) {
        if (plugin.debug(following.get(), "Followed")) {
          followingMessage.setMessage(SyncMessage.ALREADY_DONE);
        } else {
          Bukkit.getScheduler().runTask(plugin, () ->
              Bukkit.getPluginManager().callEvent(
                  new PlayerFollowEvent(user, followingMessage.getChannelId().get())));
          plugin.getSqlHelper().setFollowing(uuid, true);
        }
      }

      return new SyncResponseSuccess(
          followingMessage.getMessage(), subscriptionMessage.getMessage());
    } catch (Exception ex) {
      // TODO: More information based on error type
      plugin.debug(ex);
      return null;
    }
  }
  */

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
    headers.put("Authorization", "OAuth " + token.getAccessToken());

    return channels.entrySet().stream()
        .map(entry -> getIndividualState(entry.getValue(), headers,
            "https://api.twitch.tv/kraken/users/" + token.getId() + "/" + type + "/", entry.getKey()))
        .peek(s -> plugin.debug(s, "Peek"));
  }

  private StateWithId getIndividualState(Channel channel, Map<String, String> headers, String url, int channelId) {
    InputStreamReader reader = WebUtil.getURL(url + channelId, headers);
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

  public CompletableFuture<Void> sync(UUID uuid) {
    return CompletableFuture.runAsync(() -> plugin.getSqlHelper().getToken(uuid).ifPresent(token -> sync(uuid, token).join()));
  }

  public CompletableFuture<SyncResponse> sync(UUID uuid, Token token) {
    return CompletableFuture.<SyncResponse>supplyAsync(() -> {
      if (token == null) {
        return null;
      }

      token.setAccessToken(refreshAndSaveToken(uuid, token));

      Stream<StateWithId> states = getStates(token, channels, "subscriptions");
      SqlHelper helper = plugin.getSqlHelper();
      Permission perms = plugin.getPerms();
      states.forEach(state -> {
        Boolean subscribed = null;
        if (state.getState() == CheckState.YES) subscribed = true;
        else if (state.getState() == CheckState.NO) subscribed = false;
        if (subscribed != null) {
          helper.setSubscribed(uuid, state.getChannel().getName(), subscribed);
          OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
          String rank = state.getChannel().getSubscribeOptions().getRank();
          if (subscribed) {
            if (!perms.playerInGroup(null, player, rank)) {
              perms.playerAddGroup(null, player, rank);
            }
          } else {
            if (perms.playerInGroup(null, player, rank)) {
              perms.playerRemoveGroup(null, player, rank);
            }
          }
        }
      });
      // todo: compare with database
      // todo: check each channel and sync roles
      // todo: check total subscription count for upgrades
      return null;
    });
  }

  private AccessToken refreshAndSaveToken(UUID uuid, Token token) {
    AccessToken refresh = refreshToken(token.getAccessToken().getRefreshToken());
    plugin.getSqlHelper().setToken(uuid, token.getId(), refresh);
    return refresh;
  }

}
