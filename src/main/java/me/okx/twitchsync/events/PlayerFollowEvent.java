package me.okx.twitchsync.events;

import me.okx.twitchsync.data.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerFollowEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();

  private Channel channel;

  /**
   * Fired when a player subscription event is triggered
   *
   * @param who The player who subscribed
   */
  public PlayerFollowEvent(Player who, Channel channel) {
    super(who);
    this.channel = channel;
  }

  /**
   * @return The channel ID the user has subscribed to.
   */
  public Channel getChannel() {
    return channel;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
