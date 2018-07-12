package me.okx.twitchsync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.nimbusds.oauth2.sdk.ParseException;
import com.okta.jwt.JoseException;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtHelper;
import com.okta.jwt.JwtVerifier;
import me.okx.twitchsync.data.CheckState;
import me.okx.twitchsync.data.MessageWithId;
import me.okx.twitchsync.data.StateWithId;
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
import me.okx.twitchsync.util.WebUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;

public class Validator {
  private Gson gson = new Gson();
  private Map<Integer, String> channels;
  private TwitchSync plugin;
  private Cache<UUID, UUID> userStates;

  public Validator(TwitchSync plugin) {
    this.userStates = CacheBuilder.newBuilder()
        .expireAfterWrite(plugin.getConfig().getInt("expiry-time"), TimeUnit.MINUTES)
        .build();
    this.plugin = plugin;
    CompletableFuture.runAsync(() -> {
      this.channels = getChannelIds(plugin.getConfig().getStringList("channel-names"));
    });
  }

  public String getChannelName(int channelId) {
    return channels.get(channelId);
  }

  private Map<Integer, String> getChannelIds(List<String> channelNames) {
    Map<String, String> headers = new HashMap<>();

    headers.put("Client-ID", plugin.getConfig().getString("client-id"));
    headers.put("Accept", "application/vnd.twitchtv.v5+json");

    Reader reader = WebUtil.getURL("https://api.twitch.tv/kraken/users?login="
        + String.join(",", channelNames), headers);
    Users users = plugin.debug(gson.fromJson(reader, Users.class), "Users");

    if (users.getTotal() == 0) {
      plugin.getLogger().log(Level.SEVERE, "No channels found");
      return null;
    }

    Map<Integer, String> channels = new HashMap<>();
    for(User user : users.getUsers()) {
      channels.put(user.getId(), user.getName());
    }
    return channels;
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

    if(!subscribed.isPresent() || !following.isPresent()) {
      return new SyncResponseFailure(SyncMessage.UNKNOWN_ERROR);
    }

    Player user = Bukkit.getPlayer(uuid);

    if (user == null) {
      return new SyncResponseFailure(SyncMessage.PLAYER_NOT_FOUND);
    }

    AccessToken token;
    try {
      token = plugin.debug(getAccessToken(code));
    } catch(Exception ex) {
      plugin.debug(ex);
      return new SyncResponseFailure(SyncMessage.UNKNOWN_ERROR);
    }


    try {
      String userId = plugin.debug(getUserId(token), "User ID");
      plugin.getSqlHelper().setToken(uuid, userId, token.getAccessToken(), token.getRefreshToken());

      MessageWithId subscriptionMessage = getSubscriptionMessage(userId, token);
      if (subscriptionMessage.getMessage() == SyncMessage.SUBSCRIPTION_SUCCESS) {
        if(plugin.debug(subscribed.get(), "Subscribed")) {
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
        if(plugin.debug(following.get(), "Followed")) {
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

  private MessageWithId getSubscriptionMessage(String userId, AccessToken token) {
    try {
      StateWithId subscriptionState = plugin.debug(
          getSubscriptionState(userId, token).sorted().findFirst().get(),
          "Subscribe state");

      return new MessageWithId(
          mapState(subscriptionState.getState(), SyncMessage.SUBSCRIPTION_SUCCESS),
          subscriptionState.getId());
    } catch (Exception ex) {
      // TODO: More information based on error type
      plugin.debug(ex);
    }
    return new MessageWithId(SyncMessage.UNKNOWN_ERROR);
  }

  private MessageWithId getFollowingMessage(String userId, AccessToken token) {
    try {
      StateWithId followingState = plugin.debug(
          getFollowingState(userId, token).sorted().findFirst().get(),
          "Follow state");

      return new MessageWithId(
          mapState(followingState.getState(), SyncMessage.FOLLOW_SUCCESS),
          followingState.getId());
    } catch (Exception ex) {
      // TODO: More information based on error type
      plugin.debug(ex);
    }
    return new MessageWithId(SyncMessage.UNKNOWN_ERROR);
  }

  private SyncMessage mapState(CheckState state, SyncMessage success) {
    switch (state) {
      case YES:
        return success;
      case NO:
        return SyncMessage.NOT_BOTH;
      case UNPROCESSABLE:
        return SyncMessage.NO_SUBSCRIPTION_PROGRAM;
      default:
        return null;
    }
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

  private String getUserId(AccessToken token) throws IOException, ParseException, JoseException {
    JwtVerifier verifier = new JwtHelper()
        .setIssuerUrl("https://id.twitch.tv/oauth2")
        .setConnectionTimeout(2000)
        .setReadTimeout(2000)
        .setClientId(plugin.getConfig().getString("client-id"))
        .build();

    Jwt jwt = verifier.decodeIdToken(token.getIdToken(), null);
    return (String) jwt.getClaims().get("sub");
  }

  public Stream<StateWithId> getSubscriptionState(String userId, AccessToken token) {
    return getStates(userId, token, "subscriptions");
  }

  public Stream<StateWithId> getFollowingState(String userId, AccessToken token) {
    return getStates(userId, token, "follows/channels");
  }

  private Stream<StateWithId> getStates(String userId, AccessToken token, String type) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Client-ID", plugin.getConfig().getString("client-id"));
    headers.put("Accept", "application/vnd.twitchtv.v5+json");
    headers.put("Authorization", "OAuth " + token.getAccessToken());

    return channels.keySet().stream()
        .map(channelId -> getIndividualState(headers,
            "https://api.twitch.tv/kraken/users/" + userId + "/" + type + "/", channelId))
        .peek(s -> plugin.debug(s, "Peek"));
  }

  private StateWithId getIndividualState(Map<String, String> headers, String url, int channelId) {
    InputStreamReader reader = WebUtil.getURL(url + channelId, headers);

    ChannelObject object = gson.fromJson(reader, ChannelObject.class);
    if (object.isValid()) {
      return new StateWithId(CheckState.YES, channelId);
    }

    try {
      reader.reset();
    } catch (IOException e) {
      e.printStackTrace();
      return new StateWithId(CheckState.ERROR, channelId);
    }

    // user is not subscribed
    CheckError error = gson.fromJson(reader, CheckError.class);
    switch (error.getStatus()) {
      case 404:
        return new StateWithId(CheckState.NO, channelId);
      case 422:
        return new StateWithId(CheckState.UNPROCESSABLE, channelId);
      default:
        plugin.debug("Check state: " + error);
        return new StateWithId(CheckState.ERROR, channelId);
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
}
