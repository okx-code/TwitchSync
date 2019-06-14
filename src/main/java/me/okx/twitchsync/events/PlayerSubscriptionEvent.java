package me.okx.twitchsync.events;

import me.okx.twitchsync.data.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerSubscriptionEvent extends SyncEvent {
  private static final HandlerList handlers = new HandlerList();

  /**
   * Fired when a player subscription event is triggered
   *
   * @param who The player who subscribed
   */
  public PlayerSubscriptionEvent(Player who, Channel channel) {
    super(who, channel);
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
