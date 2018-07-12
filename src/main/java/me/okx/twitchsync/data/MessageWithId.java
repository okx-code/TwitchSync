package me.okx.twitchsync.data;

import me.okx.twitchsync.data.sync.SyncMessage;

import java.util.Optional;

public class MessageWithId {
  private SyncMessage message;
  private Optional<Integer> channelId;

  public MessageWithId(SyncMessage message, int channelId) {
    this.message = message;
    this.channelId = Optional.of(channelId);
  }

  public MessageWithId(SyncMessage message) {
    this.message = message;
    this.channelId = Optional.empty();
  }

  public Optional<Integer> getChannelId() {
    return channelId;
  }

  public SyncMessage getMessage() {
    return message;
  }

  public void setMessage(SyncMessage message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "MessageWithId{message = " + message + ", channelId = " + channelId + "}";
  }
}
