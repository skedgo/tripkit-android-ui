package com.skedgo.tripkit.ui.timetables;

import android.content.Context;
import android.text.TextUtils;
import androidx.core.util.Pair;
import com.google.gson.*;
import com.skedgo.tripkit.common.model.Region;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.utils.HttpUtils;
import com.skedgo.tripkit.routing.Shape;
import timber.log.Timber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Deprecated
public class ServiceStopFetcher {

  public static List<Shape> getShapes(Context context, String serviceTripId, Region region, long time) {
    if (TextUtils.isEmpty(serviceTripId) || region == null) {
      Timber.e("ServiceTripID or region were null!");
      return null;
    }

    final String path = context.getString(R.string.api_service);

    final List<Pair<String, Object>> params = new ArrayList<>(3);
    params.add(new Pair<>("region", region.getName()));
    params.add(new Pair<>("serviceTripID", serviceTripId));
    params.add(new Pair<>("embarkationDate", time));
    params.add(new Pair<>("encode", "true"));

    List<String> urls = region.getURLs(null);

    final Gson gson = new Gson();
    for (String url : urls) {
      try {
        final String responseStr = HttpUtils.get(url + path, params);
        JsonObject json = new JsonParser().parse(responseStr).getAsJsonObject();

        JsonArray shapesArray = json.getAsJsonArray("shapes");
        if (shapesArray != null && shapesArray.size() > 0) {
          List<Shape> shapes = new LinkedList<Shape>();
          for (int i = 0, len = shapesArray.size(); i < len; i++) {
            shapes.add(gson.fromJson(shapesArray.get(i).getAsJsonObject(), Shape.class));
          }

          return shapes;
        }
      } catch (IOException e) {
        Timber.e("Network exception when fetching service stops", e);
      } catch (JsonSyntaxException e) {
        Timber.e("Parsing exception when getting service stops", e);
      } catch (Exception e) {
        Timber.e("Unexpected exception", e);
      }
    }

    return null;
  }
}