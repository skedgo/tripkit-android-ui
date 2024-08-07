package com.skedgo.tripkit.ui.geocoding;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.ScheduledStop;
import com.skedgo.tripkit.common.util.ListUtils;

import java.lang.reflect.Type;
import java.util.List;

public class GeocodeResultAdapter implements JsonDeserializer<Location> {

    private static final String KEY_REVIEW_SUMMARIES = "reviewSummaries";
    private static final String KEY_CLASS = "class";
    private static final String KEY_SOURCES = "sources";
    private static final String VALUE_CLASS_STOP_LOCATION = "StopLocation";

    private Gson mGson;
    private TypeToken<List<String>> mSourceListTypeToken = new TypeToken<List<String>>() {
    };
    private TypeToken<List<ReviewSummary>> mReviewSummaryListTypeToken = new TypeToken<List<ReviewSummary>>() {
    };

    public GeocodeResultAdapter(Gson gson) {
        mGson = gson;
    }

    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (mGson == null) {
            return null;
        }

        JsonObject locationJson = json.getAsJsonObject();

        JsonPrimitive locationClass = locationJson.getAsJsonPrimitive(KEY_CLASS);
        if (locationClass != null && VALUE_CLASS_STOP_LOCATION.equals(locationClass.getAsString())) {
            return mGson.fromJson(locationJson, ScheduledStop.class);
        } else {
            return parseNormalLocation(locationJson);
        }
    }

    private Location parseNormalLocation(JsonObject locationJson) {
        Location location = mGson.fromJson(locationJson, Location.class);
        if (location != null) {
            JsonElement sourceListJson = locationJson.get(KEY_SOURCES);
            extractFirstSource(sourceListJson, location);

            JsonElement reviewSummaryListJson = locationJson.get(KEY_REVIEW_SUMMARIES);
            extractFirstReviewSummary(reviewSummaryListJson, location);
        }

        return location;
    }

    private void extractFirstSource(JsonElement sourceListJson, Location location) {
        List<String> sourceList = mGson.fromJson(sourceListJson, mSourceListTypeToken.getType());
        if (!ListUtils.isEmpty(sourceList)) {
            String firstSource = sourceList.get(0);
            location.setSource(firstSource);
        }
    }

    private void extractFirstReviewSummary(JsonElement reviewSummaryListJson, Location location) {
        List<ReviewSummary> reviewSummaryList = mGson.fromJson(reviewSummaryListJson, mReviewSummaryListTypeToken.getType());
        if (!ListUtils.isEmpty(reviewSummaryList)) {
            ReviewSummary firstReviewSummary = reviewSummaryList.get(0);
            location.setAverageRating(firstReviewSummary.averageRating);
            location.setRatingCount(firstReviewSummary.reviewCount);
            location.setRatingImageUrl(firstReviewSummary.ratingImageURL);
        }
    }
}