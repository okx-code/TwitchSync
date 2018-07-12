package me.okx.twitchsync.events;

import me.okx.twitchsync.TwitchSync;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerListener implements Listener {
  private TwitchSync plugin;

  public PlayerListener(TwitchSync plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void on(PlayerSubscriptionEvent e) {
    handle("subscribe", e.getPlayer(), e.getChannelId());
  }

  @EventHandler
  public void on(PlayerFollowEvent e) {
    handle("follow", e.getPlayer(), e.getChannelId());
  }

  private void handle(String path, Player player, int channelId) {
    ConfigurationSection config = plugin.getConfig().getConfigurationSection(path);
    if(!config.getBoolean("enabled")) {
      return;
    }

    String channel = plugin.getValidator().getChannelName(channelId);

    String group = config.getString("rank");
    if (!group.equalsIgnoreCase("none") && plugin.getPerms() != null) {
      plugin.getPerms().playerAddGroup(player, group);
    }

    for (String command : config.getStringList("commands")) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
          .replace("%name%", player.getName())
          .replace("%channel%", channel)
          .replace("%channelid%", channelId + ""));
    }
  }
}
