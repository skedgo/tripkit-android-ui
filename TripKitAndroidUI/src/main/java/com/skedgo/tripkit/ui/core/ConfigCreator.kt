package com.skedgo.tripkit.ui.core

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.skedgo.tripkit.TripPreferences
import com.skedgo.tripkit.agenda.ConfigRepository

/**
 * Represents configuration parameters.
 *
 *
 * See: https://redmine.buzzhives.com/projects/buzzhives/wiki/Main_API_formats#Default-configuration-parameters
 */
// TODO Is this necessary? it should not be injected, but providded by the SDK user
class ConfigCreator(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val apiVersion: String,
    private val tripPreferences: TripPreferences
) : ConfigRepository {
    override fun call(): JsonObject {
        val walkingSpeed = preferences.getString("walkingSpeed", "1")
        val cyclingSpeed = preferences.getString("cyclingSpeed", "1")
        val unit = preferences.getString("unit", "auto")
        val transferTime = preferences.getString("transferTime", "0")
        val weight = makeWeight(context, preferences)

        val json = JsonObject()
        json.addProperty("ws", walkingSpeed!!.toInt())
        json.addProperty("cs", cyclingSpeed!!.toInt())
        json.addProperty("tt", transferTime!!.toInt())
        json.addProperty("unit", unit)
        json.addProperty("v", apiVersion.toInt())
        json.addProperty("wp", weight)
        json.addProperty("ir", true)
        if (tripPreferences.isWheelchairPreferred()) {
            json.addProperty("wheelchair", true)
        }
        return json
    }

    private fun makeWeight(context: Context, preferences: SharedPreferences): String {
        // money, carbon, time, hassle.
        val budget = preferences.getInt("budget", 1)
        val carbon = preferences.getInt("carbon", 1)
        var time = preferences.getInt("time", 1)
        if (time == 0) {
            time = 1 // To satisfy the constraint that time > 0.0.
        }
        val hassle = preferences.getInt("hassle", 1)
        val sum = budget + carbon + hassle + time

        val budgetProportion = getProportion(budget, sum)
        val carbonProportion = getProportion(carbon, sum)
        val timeProportion = getProportion(time, sum)
        val hassleProportion = getProportion(hassle, sum)
        return String.format(
            "(%s,%s,%s,%s)",
            budgetProportion,
            carbonProportion,
            timeProportion,
            hassleProportion
        )
    }

    private fun getProportion(n: Int, sum: Int): String {
        return (n * 1.0 / sum).toString()
    }
}