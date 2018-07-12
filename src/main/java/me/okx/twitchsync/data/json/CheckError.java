package me.okx.twitchsync.data.json;

public class CheckError {
  private String error;
  private String message;
  private int status;

  public int getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "CheckError{error = " + error + ", message = " + message + ", status = " + status + "}";
  }
}
