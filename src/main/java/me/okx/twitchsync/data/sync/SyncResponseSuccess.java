package me.okx.twitchsync.data.sync;

public class SyncResponseSuccess extends SyncResponse {
  private SyncMessage follow;
  private SyncMessage subscribe;

  public SyncResponseSuccess(SyncMessage follow, SyncMessage subscribe) {
    this.follow = follow;
    this.subscribe = subscribe;
  }

  public boolean isFollowing() {
    return follow == SyncMessage.FOLLOW_SUCCESS;
  }

  public SyncMessage getFollowMessage() {
    return follow;
  }

  public boolean isSubscribed() {
    return subscribe == SyncMessage.SUBSCRIPTION_SUCCESS;
  }

  public SyncMessage getSubscribeMessage() {
    return subscribe;
  }
}
