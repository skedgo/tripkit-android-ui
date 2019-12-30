package com.skedgo.tripkit.ui.geocoding;

import com.skedgo.tripkit.common.model.Location;
import com.skedgo.geocoding.GCFoursquareResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO: Use OkHttp for requests.
public class FoursquareGeocoder {
  private static final String FOURSQUARE_CLIENT_ID = "0QZSSYNBJL1SC3KG45OIO41PIMQIHEB10V2HBSBMUGLZMVYZ";
  private static final String FOURSQUARE_CLIENT_SECRET = "NZJOMT2ULWYDOJCN0UHUSIXWALP2AQ3NI4WXY5X0LKEY5HNR";
  private final String input;
  private double nearbyLat = Double.MAX_VALUE;
  private double nearbyLon = Double.MAX_VALUE;

  public FoursquareGeocoder(String input, double nearbyLat, double nearbyLon) {
    this.nearbyLat = nearbyLat;
    this.nearbyLon = nearbyLon;
    this.input = input;
  }

  public List<Location> getLocationsFromFoursquare() {
    List<FoursquareResultLocationAdapter> gcList = getFromFoursquare();
    List<Location> locations = new ArrayList<>(gcList.size());

    for (FoursquareResultLocationAdapter gcLocation : gcList) {
      locations.add(gcLocation.getPlace().getLocation());
    }

    return locations;
  }

  public List<FoursquareResultLocationAdapter> getFromFoursquare() {
    ArrayList<FoursquareResultLocationAdapter> results = new ArrayList<>();

    HttpURLConnection connection = null;
    InputStream stream = null;
    StringBuilder jsonResults = new StringBuilder();
    boolean errorAtConnection = false;
    try {
      URL url = getFoursquareUrl();
      connection = (HttpURLConnection) url.openConnection();

      stream = connection.getInputStream();
      InputStreamReader in = new InputStreamReader(stream);

      // Load the results into a StringBuilder
      int read;
      char[] buff = new char[1024];
      while ((read = in.read(buff)) != -1) {
        jsonResults.append(buff, 0, read);
      }
    } catch (Exception e) {
      Timber.e(e);
      errorAtConnection = true;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
          Timber.e(e);
        }
      }
    }

    if (!errorAtConnection) {
      try {
        // Create a JSON object hierarchy from the results
        JSONObject jsonObj = new JSONObject(jsonResults.toString());
        JSONObject responseJson = jsonObj.getJSONObject("response");
        JSONArray venuesJson = responseJson.getJSONArray("venues");

        results = getFoursquareLocations(venuesJson);
      } catch (JSONException e) {
        Timber.e(e);
      }
    }
    return results;
  }

  private ArrayList<FoursquareResultLocationAdapter> getFoursquareLocations(JSONArray venuesJson) throws JSONException {
    ArrayList<FoursquareResultLocationAdapter> locations = new ArrayList<>(venuesJson.length());
    for (int i = 0; i < venuesJson.length(); i++) {
      JSONObject venueJson = venuesJson.getJSONObject(i);
      locations.add(createGCFoursquareResult(venueJson));
    }
    return locations;
  }

  private FoursquareResultLocationAdapter createGCFoursquareResult(JSONObject choice) {
    try {
      String name = choice.getString("name");
      JSONObject location = choice.getJSONObject("location");
      double lat = location.getDouble("lat");
      double lng = location.getDouble("lng");
      boolean verified = choice.optBoolean("verified", false);

      JSONArray jsonCategories = choice.getJSONArray("categories");
      List<String> categories = new ArrayList<>();
      for (int i = 0; i < jsonCategories.length(); i++) {
        JSONObject category = (JSONObject) jsonCategories.get(i);
        categories.add(category.getString("shortName"));
      }

      Location loc = new Location(lat, lng);
      String address = choice.optString("address");

      loc.setAddress(address);
      loc.setName(name);
      loc.setSource(Location.FOURSQUARE);

      GCFoursquareResult result = new GCFoursquareResult(name, lat, lng, verified, categories);

      return new FoursquareResultLocationAdapter(loc, result);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  private URL getFoursquareUrl() throws UnsupportedEncodingException, MalformedURLException {
    Date date = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    String dateString = simpleDateFormat.format(date);
    StringBuilder sb = new StringBuilder("https://api.foursquare.com/v2/venues/search?ll=");
    if (nearbyLat != Double.MAX_VALUE || nearbyLon != Double.MAX_VALUE) {
      sb.append(nearbyLat).append(",").append(nearbyLon);
    } else {
      sb.append("-33.892387,151.187315"); //sydney
    }
    sb.append("&query=" + URLEncoder.encode(input, "utf8"));
    sb.append(("&client_id=" + FOURSQUARE_CLIENT_ID));
    sb.append(("&client_secret=" + FOURSQUARE_CLIENT_SECRET));
    sb.append("&v=").append(dateString);
    return new URL(sb.toString());
  }
}
