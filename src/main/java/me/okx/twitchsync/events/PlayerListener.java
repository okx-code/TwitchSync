package me.okx.twitchsync.events;

import me.okx.twitchsync.TwitchSync;
import me.okx.twitchsync.data.Channel;
import me.okx.twitchsync.data.OptionSupplier;
import me.okx.twitchsync.data.Options;
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
    handle(Channel::getSubscribeOptions, e.getPlayer(), e.getChannel());
  }

  @EventHandler
  public void on(PlayerFollowEvent e) {
    handle(Channel::getFollowOptions, e.getPlayer(), e.getChannel());
  }

  private void handle(OptionSupplier optionSupplier, Player player, Channel channel) {
    Options options = optionSupplier.supply(channel);
    if(!options.getEnabled()) {
      return;
    }

    String group = options.getRank();
    if (!group.equalsIgnoreCase("none") && plugin.getPerms() != null) {
      plugin.getPerms().playerAddGroup(null, player, group);
    }

    for (String command : options.getCommands()) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
          .replace("%name%", player.getName())
          .replace("%channel%", channel.getName()));
    }
  }
}
