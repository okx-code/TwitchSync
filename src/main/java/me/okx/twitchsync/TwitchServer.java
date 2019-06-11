package me.okx.twitchsync;

import com.sun.net.httpserver.HttpServer;
import me.okx.twitchsync.data.sync.SyncMessage;
import me.okx.twitchsync.data.sync.SyncResponse;
import me.okx.twitchsync.data.sync.SyncResponseFailure;
import me.okx.twitchsync.data.sync.SyncResponseSuccess;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TwitchServer {
  private TwitchSync plugin;
  private int port;

  private HttpServer server;

  public TwitchServer(TwitchSync plugin) {
    this.plugin = plugin;
    this.port = plugin.getConfig().getInt("server-port");
  }

  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", ex -> {
      try {
        plugin.debug("Handling request " + ex + " on " + Thread.currentThread().getName());

        SyncResponse response = new SyncResponseFailure(SyncMessage.INVALID_URL);

        String query = ex.getRequestURI().getQuery();
        if (query != null) {
          Map<String, String> parameters = getParameters(query);

          String state = parameters.get("state");

          String code = parameters.get("code");

          if(state != null && code != null) {
            plugin.getValidator().store(UUID.fromString(state), code);
            response = plugin.getValidator().sync(UUID.fromString(state), code);
          }
        }

        SyncMessage message;
        if (response instanceof SyncResponseSuccess) {
          SyncResponseSuccess success = (SyncResponseSuccess) response;
          if (success.isFollowing() && success.isSubscribed()) {
            message = SyncMessage.BOTH_SUCCESS;
          } else if (success.isFollowing()) {
            message = SyncMessage.FOLLOW_SUCCESS;
          } else if (success.isSubscribed()) {
            message = SyncMessage.SUBSCRIPTION_SUCCESS;
          } else {
            SyncMessage subscribe = success.getSubscribeMessage();
            SyncMessage follow = success.getFollowMessage();
            // make sure the already-done message shows up
            if(subscribe == SyncMessage.ALREADY_DONE) {
              message = subscribe;
            } else {
              message = follow;
            }
          }
        } else if (response instanceof SyncResponseFailure) {
          SyncResponseFailure failure = (SyncResponseFailure) response;
          message = failure.getMessage();
        } else {
          throw new IllegalArgumentException("Sync response must either be success or failure.");
        }

        byte[] bytes = message.getValue(plugin).getBytes("UTF-8");

        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseHeaders().add("Content-Type", "Content-Type: text/html; charset=utf-8");
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
      } catch(Exception e) {
        e.printStackTrace();
      }
    });
    server.start();
  }

  private Map<String, String> getParameters(String query) {
    Map<String, String> parameters = new HashMap<>();
    for (String part : query.split("&")) {
      String[] sides = part.split("=", 2);
      if (sides.length < 2) {
        continue;
      }

      parameters.put(sides[0], sides[1]);
    }

    return parameters;
  }

  public void stop() {
    server.stop(0);
  }
}
