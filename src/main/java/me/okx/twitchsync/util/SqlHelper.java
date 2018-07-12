package me.okx.twitchsync.util;

import me.okx.twitchsync.TwitchSync;
import me.okx.twitchsync.data.Token;
import me.okx.twitchsync.data.json.AccessToken;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SqlHelper {
  private Connection connection;

  public SqlHelper(TwitchSync plugin) {
    Path db = plugin.debug(plugin.getDataFolder().toPath().resolve("synced.db"), "DB");

    CompletableFuture.runAsync(() -> {
      try {
        connection = DriverManager.getConnection("jdbc:sqlite:" + db);

        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS subscribed (uuid VARCHAR(36) PRIMARY KEY)");
        stmt.execute("CREATE TABLE IF NOT EXISTS following  (uuid VARCHAR(36) PRIMARY KEY)");
        stmt.execute("CREATE TABLE IF NOT EXISTS tokens " +
            "(uuid VARCHAR(36) PRIMARY KEY, " +
            "id VARCHAR(12), " +
            "access_token VARCHAR(32), " +
            "refresh_token VARCHAR(48))");
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  public Optional<Integer> getFollowing() {
    return getCount("following");
  }

  public Optional<Integer> getSubscribed() {
    return getCount("subscribed");
  }

  private Optional<Integer> getCount(String table) {
    try(PreparedStatement stmt = connection.prepareStatement(
        "SELECT * FROM " + table)) {
      ResultSet rs = stmt.executeQuery();
      int count = 0;
      while(rs.next()) {
        count++;
      }
      return Optional.of(count);
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<Boolean> isFollowing(UUID uuid) {
    return isInTable(uuid, "following");
  }

  public Optional<Boolean> isSubscribed(UUID uuid) {
    return isInTable(uuid, "subscribed");
  }

  private Optional<Boolean> isInTable(UUID uuid, String table) {
    try(PreparedStatement stmt = connection.prepareStatement(
        "SELECT * FROM " + table + " WHERE uuid=?")) {
      stmt.setString(1, uuid.toString());
      stmt.execute();
      return Optional.of(stmt.getResultSet().next());
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public void setToken(UUID uuid, String id, String accessToken, String refeshToken) {
    CompletableFuture.runAsync(() -> {
      try(PreparedStatement stmt = connection.prepareStatement(
          "REPLACE INTO tokens (uuid, id, access_token, refresh_token) VALUES (?, ?, ?, ?)")) {
        stmt.setString(1, uuid.toString());
        stmt.setString(2, id);
        stmt.setString(3, accessToken);
        stmt.setString(4, refeshToken);
        stmt.execute();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  public Optional<Map<UUID, Token>> getTokens() {
    try(PreparedStatement stmt = connection.prepareStatement("SELECT * FROM tokens")) {
      ResultSet rs = stmt.executeQuery();

      Map<UUID, Token> tokens = new HashMap<>();

      while(rs.next()) {
        tokens.put(UUID.fromString(rs.getString("uuid")),
            new Token(rs.getString("id"),
                new AccessToken(rs.getString("access_token"), rs.getString("refresh_token"))));
      }

      return Optional.of(tokens);
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public void setFollowing(UUID uuid, boolean following) {
    CompletableFuture.runAsync(() -> {
      if (following) {
        addToTable(uuid, "following");
      } else {
        deleteFromTable(uuid, "following");
      }
    });
  }

  public void setSubscribed(UUID uuid, boolean subscribed) {
    CompletableFuture.runAsync(() -> {
      if (subscribed) {
        addToTable(uuid, "subscribed");
      } else {
        deleteFromTable(uuid, "subscribed");
      }
    });
  }

  private void addToTable(UUID uuid, String database) {
    try(PreparedStatement stmt = connection.prepareStatement(
        "INSERT INTO " + database + " (uuid) VALUES (?)")) {
      stmt.setString(1, uuid.toString());
      stmt.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void deleteFromTable(UUID uuid, String database) {
    try(PreparedStatement stmt = connection.prepareStatement(
        "DELETE FROM " + database + " WHERE uuid=?")) {
      stmt.setString(1, uuid.toString());
      stmt.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
