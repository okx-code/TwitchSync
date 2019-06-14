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
    plugin.getLogger().info("Syncing user data now.");
    long start = System.currentTimeMillis();

    Map<UUID, Token> tokens = plugin.getSqlHelper().getTokens().get();
    plugin.getValidator().sync(tokens);

    long time = System.currentTimeMillis() - start;
    plugin.getLogger().info("Finished Sync in " + time + "ms.");
  }

}
