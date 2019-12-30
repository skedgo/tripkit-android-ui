package com.skedgo.tripkit.ui.tripresult;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.Region;
import com.skedgo.tripkit.common.util.Gsons;
import com.skedgo.tripkit.common.util.ListUtils;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.model.TimetableEntry;
import com.skedgo.tripkit.ui.utils.HttpUtils;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import com.skedgo.tripkit.agenda.ConfigRepository;
import com.skedgo.tripkit.routing.RoutingResponse;
import com.skedgo.tripkit.routing.SegmentType;
import com.skedgo.tripkit.routing.TripGroup;
import com.skedgo.tripkit.routing.TripSegment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * https://redmine.buzzhives.com/projects/buzzhives/wiki/Routing_API#Trips-from-waypoint
 */
public class WaypointTask implements SingleOnSubscribe<List<TripGroup>> {
  public static final String KEY_REGION = "region";
  public static final String KEY_SEGMENTS = "segments";
  public static final String KEY_OPERATOR = "operator";
  public static final String KEY_SERVICE_TRIP_ID = "serviceTripID";
  public static final String KEY_END_TIME = "endTime";
  public static final String KEY_START_TIME = "startTime";
  public static final String KEY_MODES = "modes";
  public static final String KEY_END = "end";
  public static final String KEY_START = "start";
  public static final String KEY_CONFIG = "config";
  public static final String FORMAT_COORDINATES = "(%f,%f)";

  private final Context context;
  private final ConfigRepository configCreator;
  private WayPointTaskParam param;

  public WaypointTask(
      @NonNull Context context,
      @NonNull ConfigRepository configCreator,
      WayPointTaskParam param) {
    this.context = context;
    this.configCreator = configCreator;
    this.param = param;
  }

  static JsonArray createJsonSegments(ArrayList<TripSegment> segments,
                                      TripSegment prototypeSegment,
                                      Location waypoint,
                                      boolean isGetOn) {
    boolean changeNextDeparture = false;
    boolean isTimeAdded = false;

    JsonArray jsonSegments = new JsonArray();
    for (TripSegment segment : segments) {
      if (segment.getId() == prototypeSegment.getId()) {
        JsonObject jsonSegment = new JsonObject();
        if (isGetOn) {
          // The waypoint now becomes the departure.
          jsonSegment.addProperty(KEY_START, waypoint.getCoordinateString());
          jsonSegment.addProperty(KEY_END, segment.getTo().getCoordinateString());
        } else {
          // Get-off case.
          jsonSegment.addProperty(KEY_START, segment.getFrom().getCoordinateString());

          // The waypoint now becomes the arrival.
          jsonSegment.addProperty(KEY_END, waypoint.getCoordinateString());

          // This case we have to change next segment's departure.
          changeNextDeparture = true;
        }

        if (!TextUtils.isEmpty(segment.getTransportModeId())) {
          JsonArray jsonModes = new JsonArray();
          jsonModes.add(new JsonPrimitive(segment.getTransportModeId()));
          jsonSegment.add(KEY_MODES, jsonModes);
        }

        if (!isTimeAdded) {
          jsonSegment.addProperty(KEY_START_TIME, segment.getStartTimeInSecs());

          // We only add once.
          isTimeAdded = true;
        }

        jsonSegments.add(jsonSegment);
      } else if ((segment.getType() != SegmentType.STATIONARY)
          && (segment.getType() != SegmentType.ARRIVAL)
          && (segment.getType() != SegmentType.DEPARTURE)) {
        JsonObject jsonSegment = convertSegmentToJson(segment);
        if (changeNextDeparture) {
          // We've iterated at the segment following the prototype segment.
          jsonSegment.addProperty(KEY_START, waypoint.getCoordinateString());

          // We only change once.
          changeNextDeparture = true;
        }

        if (!isTimeAdded) {
          jsonSegment.addProperty(KEY_START_TIME, segment.getStartTimeInSecs());

          // We only add once.
          isTimeAdded = true;
        }

        jsonSegments.add(jsonSegment);
      }
    }

    return jsonSegments;
  }

  static JsonObject convertSegmentToJson(TripSegment segment) {
    JsonObject jsonSegment = new JsonObject();
    jsonSegment.addProperty(KEY_START, segment.getFrom().getCoordinateString());
    jsonSegment.addProperty(KEY_END, segment.getTo().getCoordinateString());

    if (!TextUtils.isEmpty(segment.getTransportModeId())) {
      JsonArray jsonModes = new JsonArray();
      jsonModes.add(new JsonPrimitive(segment.getTransportModeId()));
      jsonSegment.add(KEY_MODES, jsonModes);
    }

    return jsonSegment;
  }

  /**
   * TODO: Handle 'vehicleUUID'.
   */
  static JsonObject createPostDataForChangingStop(JsonObject configParams,
                                                  JsonArray segments) {
    JsonObject jsonPostData = new JsonObject();
    jsonPostData.add(KEY_CONFIG, configParams);
    jsonPostData.add(KEY_SEGMENTS, segments);
    return jsonPostData;
  }

  @Override
  public void subscribe(SingleEmitter<List<TripGroup>> singleSubscriber) throws Exception {
    Region region;

    String postData;
    try {
      region = param.getRegion();
      if (param instanceof WayPointTaskParam.ForChangingService) {
        ArrayList<TripSegment> segments = ((WayPointTaskParam.ForChangingService) param).getSegments();
        TripSegment prototypeSegment = ((WayPointTaskParam.ForChangingService) param).getPrototypeSegment();
        TimetableEntry service = ((WayPointTaskParam.ForChangingService) param).getService();
        postData = createPostDataForChangingService(
            region,
            segments,
            prototypeSegment,
            service
        );
      } else {
        ArrayList<TripSegment> segments = ((WayPointTaskParam.ForChangingStop) param).getSegments();
        TripSegment prototypeSegment = ((WayPointTaskParam.ForChangingStop) param).getPrototypeSegment();
        Location waypoint = ((WayPointTaskParam.ForChangingStop) param).getWaypoint();
        boolean isGetOn = ((WayPointTaskParam.ForChangingStop) param).isGetOn();

        postData = createPostDataForChangingStop(
            configCreator.call(),
            createJsonSegments(
                segments,
                prototypeSegment,
                waypoint,
                isGetOn
            )
        ).toString();
      }
    } catch (Exception e) {
      singleSubscriber.onError(e);
      return;
    }

    List<String> serverURLs = region.getURLs();
    if (serverURLs != null) {
      for (String serverURL : serverURLs) {
        try {
          String waypointResponseBody = HttpUtils.post(serverURL + context.getString(R.string.api_waypoint), postData);
          Gson gson = Gsons.createForLowercaseEnum();
          RoutingResponse waypointResponse = gson.fromJson(waypointResponseBody, RoutingResponse.class);
          if (waypointResponse.hasError()) {
            singleSubscriber.onError(new RuntimeException(waypointResponse.getErrorMessage()));
            return;
          }

          waypointResponse.processRawData(context.getResources(), gson);
          ArrayList<TripGroup> tripGroups = waypointResponse.getTripGroupList();
          if (ListUtils.isEmpty(tripGroups) || ListUtils.isEmpty(tripGroups.get(0).getTrips())) {
            singleSubscriber.onError(new RuntimeException("No groups found"));
            return;
          }

          singleSubscriber.onSuccess(tripGroups);
          return;
        } catch (IOException e) {
          singleSubscriber.onError(e);
        }
      }
    } else {
      singleSubscriber.onError(new RuntimeException("No urls"));
    }
  }

  String createPostDataForChangingService(Region region,
                                          ArrayList<TripSegment> segments,
                                          TripSegment prototypeSegment,
                                          TimetableEntry service) {
    JsonArray jsonSegments = new JsonArray();
    for (TripSegment segment : segments) {
      if (segment.getId() == prototypeSegment.getId()) {
        JsonObject jsonSegment = convertServiceToJson(region, service);
        jsonSegments.add(jsonSegment);
      } else if ((segment.getType() != SegmentType.STATIONARY)
          && (segment.getType() != SegmentType.ARRIVAL)
          && (segment.getType() != SegmentType.DEPARTURE)) {
        JsonObject jsonSegment = convertSegmentToJson(segment);
        jsonSegments.add(jsonSegment);
      }
    }

    JsonObject jsonPostData = new JsonObject();
    jsonPostData.add(KEY_CONFIG, configCreator.call());
    jsonPostData.add(KEY_SEGMENTS, jsonSegments);

    return jsonPostData.toString();
  }

  private JsonObject convertServiceToJson(Region region, TimetableEntry service) {
    JsonObject jsonSegment = new JsonObject();
    jsonSegment.addProperty(KEY_START, service.getStopCode());
    jsonSegment.addProperty(KEY_END, service.getEndStopCode());

    JsonArray jsonModes = new JsonArray();
    jsonModes.add(new JsonPrimitive("pt_pub"));
    jsonModes.add(new JsonPrimitive("pt_sch"));
    jsonSegment.add(KEY_MODES, jsonModes);

    jsonSegment.addProperty(KEY_START_TIME, service.getStartTimeInSecs());
    jsonSegment.addProperty(KEY_END_TIME, service.getEndTimeInSecs());
    jsonSegment.addProperty(KEY_SERVICE_TRIP_ID, service.getServiceTripId());
    jsonSegment.addProperty(KEY_OPERATOR, service.getOperator());
    jsonSegment.addProperty(KEY_REGION, region.getName());
    return jsonSegment;
  }
}