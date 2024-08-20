package com.skedgo.tripkit.ui.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.skedgo.tripkit.TripPreferences;
import com.skedgo.tripkit.agenda.ConfigRepository;

import androidx.annotation.NonNull;

/**
 * Represents configuration parameters.
 * <p/>
 * See: https://redmine.buzzhives.com/projects/buzzhives/wiki/Main_API_formats#Default-configuration-parameters
 */

// TODO Is this necessary? it should not be injected, but providded by the SDK user
public final class ConfigCreator implements ConfigRepository {
    private final Context context;
    private final SharedPreferences preferences;
    private final String apiVersion;
    private final TripPreferences tripPreferences;

    public ConfigCreator(
        Context context,
        SharedPreferences preferences,
        String apiVersion,
        TripPreferences tripPreferences) {
        this.context = context;
        this.preferences = preferences;
        this.apiVersion = apiVersion;
        this.tripPreferences = tripPreferences;
    }

    @Override
    public @NonNull JsonObject call() {
        final String walkingSpeed = preferences.getString("walkingSpeed", "1");
        final String cyclingSpeed = preferences.getString("cyclingSpeed", "1");
        final String unit = preferences.getString("unit", "auto");
        final String transferTime = preferences.getString("transferTime", "0");
        final String weight = makeWeight(context, preferences);

        final JsonObject json = new JsonObject();
        json.addProperty("ws", Integer.parseInt(walkingSpeed));
        json.addProperty("cs", Integer.parseInt(cyclingSpeed));
        json.addProperty("tt", Integer.parseInt(transferTime));
        json.addProperty("unit", unit);
        json.addProperty("v", Integer.parseInt(apiVersion));
        json.addProperty("wp", weight);
        json.addProperty("ir", true);
        if (tripPreferences.isWheelchairPreferred()) {
            json.addProperty("wheelchair", true);
        }
        return json;
    }

    private String makeWeight(Context context, SharedPreferences preferences) {
        // money, carbon, time, hassle.
        int budget = preferences.getInt("budget", 1);
        int carbon = preferences.getInt("carbon", 1);
        int time = preferences.getInt("time", 1);
        if (time == 0) {
            time = 1; // To satisfy the constraint that time > 0.0.
        }
        int hassle = preferences.getInt("hassle", 1);
        int sum = budget + carbon + hassle + time;

        final String budgetProportion = getProportion(budget, sum);
        final String carbonProportion = getProportion(carbon, sum);
        final String timeProportion = getProportion(time, sum);
        final String hassleProportion = getProportion(hassle, sum);
        return String.format(
            "(%s,%s,%s,%s)",
            budgetProportion,
            carbonProportion,
            timeProportion,
            hassleProportion
        );
    }

    private String getProportion(int n, int sum) {
        return String.valueOf(n * 1.0 / sum);
    }
}