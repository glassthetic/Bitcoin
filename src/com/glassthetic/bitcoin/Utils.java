package com.glassthetic.bitcoin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
  /**
   * Utility method to fetch JSON from url as {@link JSONObject}
   * <br>
   * <strong>Note:</strong> This uses an {@link HttpsURLConnection}, so
   * urlString parameter must begin with "https".
   * 
   * @param urlString String representing the url to fetch JSON from.
   * @return {@link JSONObject}
   * @throws IOException 
   * @throws JSONException 
   */
  public static JSONObject getJson(String urlString) throws IOException, JSONException {
    URL url = new URL(urlString);
    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
    String jsonString = null;
    
    try {
      InputStream in = new BufferedInputStream(urlConnection.getInputStream());
      jsonString = IOUtils.toString(in, "UTF-8");
    } finally {
      urlConnection.disconnect();
    }
    
    return new JSONObject(jsonString);
  }
}
