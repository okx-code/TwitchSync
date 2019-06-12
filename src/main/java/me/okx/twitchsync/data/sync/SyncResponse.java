package me.okx.twitchsync.data.sync;

import me.okx.twitchsync.data.Channel;

import java.util.Collections;
import java.util.List;

public class SyncResponse {

  private SyncMessage message;
  private List<Channel> subscriptions;
  private List<Channel> follows;

  public SyncResponse(SyncMessage message, List<Channel> subscriptions, List<Channel> follows) {
    this.message = message;
    this.subscriptions = subscriptions;
    this.follows = follows;
  }

  public static SyncResponse of(SyncMessage message) {
    return new SyncResponse(message, Collections.emptyList(), Collections.emptyList());
  }

  public List<Channel> getSubscriptions() {
    return subscriptions;
  }

  public List<Channel> getFollows() {
    return follows;
  }

  public SyncMessage getMessage() {
    return message;
  }

}
