package me.okx.twitchsync.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerFollowEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();

  private int channelId;

  /**
   * Fired when a player subscription event is triggered
   *
   * @param who The player who subscribed
   */
  public PlayerFollowEvent(Player who, int channelId) {
    super(who);
    this.channelId = channelId;
  }

  /**
   * @return The channel ID the user has subscribed to.
   */
  public int getChannelId() {
    return channelId;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
