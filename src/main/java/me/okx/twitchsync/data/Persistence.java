package me.okx.twitchsync.data;

import me.okx.twitchsync.util.SqlHelper;

import java.util.UUID;

public interface Persistence {

  public void persist(SqlHelper helper, UUID uuid, String channel, Boolean state);

}
