package me.okx.twitchsync;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RevokeCommand implements CommandExecutor {
  private TwitchSync plugin;

  public RevokeCommand(TwitchSync plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender cs, Command command, String s, String[] strings) {
    new Revoker(plugin).runTaskAsynchronously(plugin);
    cs.sendMessage(ChatColor.GREEN + "Revoking those who are no longer following or subscribed.");
    return true;
  }
}
