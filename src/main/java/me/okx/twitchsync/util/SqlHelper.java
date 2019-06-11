package me.okx.twitchsync.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import me.okx.twitchsync.TwitchSync;
import me.okx.twitchsync.data.Token;
import me.okx.twitchsync.data.json.AccessToken;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
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
        stmt.execute("CREATE TABLE IF NOT EXISTS subscribed (uuid VARCHAR(36), channel VARCHAR(50), PRIMARY KEY (uuid, channel))");
        stmt.execute("CREATE TABLE IF NOT EXISTS following  (uuid VARCHAR(36), channel VARCHAR(50), PRIMARY KEY (uuid, channel))");
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

  public Optional<Boolean> isFollowing(UUID uuid, String channel) {
    return isInTable("following", uuid, channel);
  }

  public Optional<Boolean> isSubscribed(UUID uuid, String channel) {
    return isInTable("subscribed", uuid, channel);
  }

  private Optional<Boolean> isInTable(String table, UUID uuid, String channel) {
    try(PreparedStatement stmt = connection.prepareStatement(
        "SELECT * FROM " + table + " WHERE uuid = ? AND channel = ?")) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, channel);
      stmt.execute();
      return Optional.of(stmt.getResultSet().next());
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public void setToken(UUID uuid, String userId, AccessToken token) {
    CompletableFuture.runAsync(() -> {
      try(PreparedStatement stmt = connection.prepareStatement(
          "REPLACE INTO tokens (uuid, id, access_token, refresh_token) VALUES (?, ?, ?, ?)")) {
        stmt.setString(1, uuid.toString());
        stmt.setString(2, userId);
        stmt.setString(3, token.getAccessToken());
        stmt.setString(4, token.getRefreshToken());
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
    try(PreparedStatement stmt = connection.prepareStatement(
        "INSERT INTO " + table + " (uuid, channel) VALUES (?, ?)")) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, channel);
      stmt.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void deleteFromTable(String table, UUID uuid, String channel) {
    try(PreparedStatement stmt = connection.prepareStatement(
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
    try(PreparedStatement stmt = connection.prepareStatement("SELECT * FROM tokens WHERE uuid = ?")) {
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
