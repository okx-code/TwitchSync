package me.okx.twitchsync;

import me.okx.twitchsync.data.CheckState;
import me.okx.twitchsync.data.StateWithId;
import me.okx.twitchsync.data.Token;
import me.okx.twitchsync.data.json.AccessToken;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class Revoker extends BukkitRunnable  {
  private TwitchSync plugin;

  public Revoker(TwitchSync plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    plugin.getLogger().info("Revoking now.");
    long start = System.currentTimeMillis();

    Map<UUID, Token> tokens = plugin.getSqlHelper().getTokens().get();
    checkTokens(tokens);

    long time = System.currentTimeMillis() - start;
    plugin.getLogger().info("Finished revoking in " + time + "ms.");
  }

  private void checkTokens(Map<UUID, Token> tokens) {
    for(Map.Entry<UUID, Token> entry : tokens.entrySet()) {
      UUID uuid = entry.getKey();

      Token token = entry.getValue();

      AccessToken accessToken = refresh(token.getAccessToken());
      plugin.getSqlHelper().setToken(uuid,
          token.getId(),
          accessToken.getAccessToken(),
          accessToken.getRefreshToken());

      plugin.getSqlHelper().isFollowing(uuid).ifPresent(b -> {
        if(b && check(plugin.getValidator().getFollowingState(token.getId(), accessToken))) {
          revoke("follow", uuid);
          plugin.getSqlHelper().setFollowing(uuid, false);
        }
      });
      plugin.getSqlHelper().isSubscribed(uuid).ifPresent(b -> {
        // if subscribed and all states are not YES
        if(b && check(plugin.getValidator().getSubscriptionState(token.getId(), accessToken))) {
          revoke("subscribe", uuid);
          plugin.getSqlHelper().setSubscribed(uuid, false);
        }
      });
    }
  }

  private void revoke(String type, UUID uuid) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection(type);
    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

    Bukkit.getScheduler().runTask(plugin, () -> {
      plugin.debug(type + " - " + uuid, "revoking");
      String group = section.getString("rank");
      if (!group.equalsIgnoreCase("none") && plugin.getPerms() != null) {
        plugin.getPerms().playerRemoveGroup(null, player, group);
      }

      for (String command : section.getStringList("revoke-commands")) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
            .replace("%name%", player.getName()));
      }
    });
  }

  /**
   * Check if all states are not YES
   */
  public boolean check(Stream<StateWithId> states) {
    return states.allMatch(stateWithId -> stateWithId.getState() != CheckState.YES);
  }

  private AccessToken refresh(AccessToken accessToken) {
    return plugin.getValidator().refreshToken(accessToken.getRefreshToken());
  }
}
