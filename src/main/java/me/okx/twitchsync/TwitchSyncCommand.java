package me.okx.twitchsync;

import me.okx.twitchsync.data.sync.SyncMessage;
import me.okx.twitchsync.data.sync.SyncResponse;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TwitchSyncCommand implements CommandExecutor {
  private TwitchSync plugin;

  public TwitchSyncCommand(TwitchSync plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender cs, Command command, String s, String[] args) {
    if (!(cs instanceof Player)) {
      cs.sendMessage(ChatColor.RED + "You must be a player to do this");
      return true;
    }

    Player player = (Player) cs;
    plugin.getValidator().sync(player.getUniqueId()).whenCompleteAsync((response, throwable) -> {
      if (throwable != null) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6An error occurred. Please try again."));
        plugin.getLogger().warning(throwable.toString());
        throwable.printStackTrace();
      }
      if (response.getMessage() != SyncMessage.NO_TOKEN) {
        player.sendMessage(ChatColor.GREEN + "Re-synced.");
      } else {
        String url = plugin.getValidator().createAuthenticationUrl(player.getUniqueId());
        if(url == null) {
          player.sendMessage(ChatColor.RED + "An error occurred. Please try again.");
          return;
        }
        player.spigot().sendMessage(new ComponentBuilder("Click this text to sync to Twitch")
            .color(net.md_5.bungee.api.ChatColor.GREEN)
            .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to sync to Twitch").create()))
            .create());
      }
    });

    return true;
  }
}
