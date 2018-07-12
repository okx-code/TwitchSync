package me.okx.twitchsync.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class WebUtil {
  public static InputStreamReader getURL(String url) {
    return getURL(url, Collections.emptyMap());
  }

  public static InputStreamReader getURL(String url, Map<String, String> params) {
    return getURL(url, params, "GET");
  }

  public static InputStreamReader getURL(String url, Map<String, String> params, String method) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      for (Map.Entry<String, String> param : params.entrySet()) {
        connection.addRequestProperty(param.getKey(), param.getValue());
      }
      connection.setRequestMethod(method);

      InputStream stream;
      if(connection.getResponseCode() == 200) {
        stream = connection.getInputStream();
      } else {
        stream = connection.getErrorStream();
      }
      return new InputStreamReader(stream);
    } catch(IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }
}
