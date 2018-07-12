package me.okx.twitchsync.data.sync;

public class SyncResponseFailure extends SyncResponse {
  private SyncMessage message;

  public SyncResponseFailure(SyncMessage message) {
    this.message = message;
  }

  public SyncMessage getMessage() {
    return message;
  }
}
