package me.okx.twitchsync.events;

import me.okx.twitchsync.data.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public abstract class SyncEvent extends PlayerEvent {

  protected Channel channel;

  public SyncEvent(Player who, Channel channel) {
    super(who);
    this.channel = channel;
  }

  /**
   * @return The channel ID the user has subscribed to.
   */
  public Channel getChannel() {
    return channel;
  }

}
