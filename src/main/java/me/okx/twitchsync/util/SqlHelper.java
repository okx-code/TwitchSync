package me.okx.twitchsync.util;

import me.okx.twitchsync.TwitchSync;
import me.okx.twitchsync.data.Token;
import me.okx.twitchsync.data.json.AccessToken;

import java.nio.file.Path;
import java.sql.*;
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
        stmt.execute("CREATE TABLE IF NOT EXISTS subscribed (uuid VARCHAR(36), channel VARCHAR(25), PRIMARY KEY (uuid, channel))");
        stmt.execute("CREATE TABLE IF NOT EXISTS following  (uuid VARCHAR(36), channel VARCHAR(25), PRIMARY KEY (uuid, channel))");
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

  public Boolean isFollowing(UUID uuid, String channel) {
    return isInTable("following", uuid, channel);
  }

  public Boolean isSubscribed(UUID uuid, String channel) {
    return isInTable("subscribed", uuid, channel);
  }

  private Boolean isInTable(String table, UUID uuid, String channel) {
    try (PreparedStatement stmt = connection.prepareStatement(
        "SELECT * FROM " + table + " WHERE uuid = ? AND channel = ?")) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, channel);
      stmt.execute();
      return stmt.getResultSet().next();
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public CompletableFuture<Token> setToken(UUID uuid, String userId, AccessToken token) {
    return CompletableFuture.supplyAsync(() -> {
      try (PreparedStatement stmt = connection.prepareStatement(
          "REPLACE INTO tokens (uuid, id, access_token, refresh_token) VALUES (?, ?, ?, ?)")) {
        stmt.setString(1, uuid.toString());
        stmt.setString(2, userId);
        stmt.setString(3, token.getAccessToken());
        stmt.setString(4, token.getRefreshToken());
        stmt.execute();
        return new Token(userId, token);
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
    });
  }

  public Optional<Map<UUID, Token>> getTokens() {
    try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM tokens")) {
      ResultSet rs = stmt.executeQuery();

      Map<UUID, Token> tokens = new HashMap<>();

      while (rs.next()) {
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

  public void setFollowing(UUID uuid, String channel, boolean following) {
    CompletableFuture.runAsync(() -> {
      if (following) {
        addToTable("following", uuid, channel);
      } else {
        deleteFromTable("following", uuid, channel);
      }
    });
  }

  public void setSubscribed(UUID uuid, String channel, boolean subscribed) {
    CompletableFuture.runAsync(() -> {
      if (subscribed) {
        addToTable("subscribed", uuid, channel);
      } else {
        deleteFromTable("subscribed", uuid, channel);
      }
    });
  }

  private void addToTable(String table, UUID uuid, String channel) {
    try (PreparedStatement stmt = connection.prepareStatement(
        "INSERT INTO " + table + " (uuid, channel) VALUES (?, ?)")) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, channel);
      stmt.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void deleteFromTable(String table, UUID uuid, String channel) {
    try (PreparedStatement stmt = connection.prepareStatement(
        "DELETE FROM " + table + " WHERE uuid = ? AND channel = ?")) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, channel);
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

  public Optional<Token> getToken(UUID uuid) {
    try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM tokens WHERE uuid = ?")) {
      stmt.setString(1, uuid.toString());
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        Token token = new Token(rs.getString("id"),
            new AccessToken(rs.getString("access_token"), rs.getString("refresh_token")));
        return Optional.of(token);
      }
      return Optional.empty();
    } catch (SQLException ex) {
      return Optional.empty();
    }
  }
}
