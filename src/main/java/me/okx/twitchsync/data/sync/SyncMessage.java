package me.okx.twitchsync.data.sync;

import me.okx.twitchsync.TwitchSync;

public enum SyncMessage {
  NO_SUBSCRIPTION_PROGRAM("subscription.no-subscription-program"),
  STATE_NOT_FOUND("state-not-found"),
  PLAYER_NOT_FOUND("player-not-found"),
  SUBSCRIPTION_SUCCESS("subscription.success"),
  FOLLOW_SUCCESS("follow.success"),
  INVALID_URL("invalid-url"),
  UNKNOWN_ERROR("unknown-error"),
  BOTH_SUCCESS("success-both"),
  NOT_BOTH("not-both"),
  ALREADY_DONE("already-done");

  private String config;

  SyncMessage(String config) {
    this.config = config;
  }

  public String getValue(TwitchSync plugin) {
    return plugin.getConfig().getConfigurationSection("messages").getString(config);
  }
}
