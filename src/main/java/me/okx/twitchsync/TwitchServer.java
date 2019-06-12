package me.okx.twitchsync;

import com.sun.net.httpserver.HttpServer;
import me.okx.twitchsync.data.Token;
import me.okx.twitchsync.data.sync.SyncMessage;
import me.okx.twitchsync.data.sync.SyncResponse;

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

        SyncResponse response = SyncResponse.of(SyncMessage.INVALID_URL);

        String query = ex.getRequestURI().getQuery();
        if (query != null) {
          Map<String, String> parameters = getParameters(query);

          String state = parameters.get("state");

          String code = parameters.get("code");

          if(state != null && code != null) {
            Token token = plugin.getValidator().store(UUID.fromString(state), code).get();
            response = plugin.getValidator().sync(UUID.fromString(state), token).get();
          }
        }

        SyncMessage message = response.getMessage();

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
