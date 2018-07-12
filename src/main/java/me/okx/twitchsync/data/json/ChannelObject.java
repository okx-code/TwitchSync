package me.okx.twitchsync.data.json;

import com.google.gson.JsonObject;

public class ChannelObject {
  private JsonObject channel;

  public boolean isValid() {
    return channel != null;
  }
}
