package me.okx.twitchsync.data.json;

import com.google.gson.annotations.SerializedName;

public class AccessToken {
  @SerializedName("access_token")
  private String accessToken;

  @SerializedName("id_token")
  private String idToken;

  @SerializedName("refresh_token")
  private String refreshToken;

  public String getIdToken() {
    return idToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public AccessToken(String accessToken, String refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @Override
  public String toString() {
    return "AccessToken{idToken = \"" + idToken + "\", accessToken = \"" + accessToken + "\"}";
  }
}
