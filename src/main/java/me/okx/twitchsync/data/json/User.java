package me.okx.twitchsync.data.json;

public class User {
  private int _id;
  private String bio;
  private String created_at;
  private String display_name;
  private String logo;
  private String name;
  private String type;
  private String updated_at;

  public int getId() {
    return _id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "User{id=" + _id + ", name=\"" + name + "\"}";
  }
}
