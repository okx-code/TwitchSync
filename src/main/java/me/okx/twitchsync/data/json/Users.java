package me.okx.twitchsync.data.json;

public class Users {
  private int _total;
  private User[] users;

  public int getTotal() {
    return _total;
  }

  public User[] getUsers() {
    return users;
  }

  @Override
  public String toString() {
    String[] usersString = new String[users.length];
    for(int i = 0; i < users.length; i++) {
      usersString[i] = users[i].toString();
    }

    return "Users{total=" + _total + ", " +
        "users=[" + String.join(", ", usersString) + "]}";
  }
}
