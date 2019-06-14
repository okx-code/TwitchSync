package me.okx.twitchsync.data;

import me.okx.twitchsync.data.json.AccessToken;

public class Token {
  private String id;
  private AccessToken accessToken;

  public String getId() {
    return id;
  }

  public AccessToken getAccessToken() {
    return accessToken;
  }

  public Token(String id, AccessToken accessToken) {
    this.id = id;
    this.accessToken = accessToken;
  }

  public void setAccessToken(AccessToken token) {
    this.accessToken = token;
  }

  public void setId(String userId) {
    this.id = userId;
  }
}
