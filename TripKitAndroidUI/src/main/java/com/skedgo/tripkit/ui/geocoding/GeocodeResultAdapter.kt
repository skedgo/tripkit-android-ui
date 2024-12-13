package com.skedgo.tripkit.ui.geocoding

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.util.ListUtils
import java.lang.reflect.Type

class GeocodeResultAdapter(private val mGson: Gson?) : JsonDeserializer<Location> {
    private val mSourceListTypeToken: TypeToken<List<String>> =
        object : TypeToken<List<String>>() {
        }
    private val mReviewSummaryListTypeToken: TypeToken<List<ReviewSummary>> =
        object : TypeToken<List<ReviewSummary>>() {
        }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Location? {
        if (mGson == null) {
            return null
        }

        val locationJson = json.asJsonObject

        val locationClass = locationJson.getAsJsonPrimitive(KEY_CLASS)
        return if (locationClass != null && VALUE_CLASS_STOP_LOCATION == locationClass.asString) {
            mGson.fromJson(locationJson, ScheduledStop::class.java)
        } else {
            parseNormalLocation(locationJson)
        }
    }

    private fun parseNormalLocation(locationJson: JsonObject): Location {
        val location = mGson!!.fromJson(locationJson, Location::class.java)
        if (location != null) {
            val sourceListJson = locationJson[KEY_SOURCES]
            extractFirstSource(sourceListJson, location)

            val reviewSummaryListJson = locationJson[KEY_REVIEW_SUMMARIES]
            extractFirstReviewSummary(reviewSummaryListJson, location)
        }

        return location
    }

    private fun extractFirstSource(sourceListJson: JsonElement, location: Location) {
        val sourceList = mGson?.fromJson<List<String>>(sourceListJson, mSourceListTypeToken.type).orEmpty()
        if (!ListUtils.isEmpty(sourceList)) {
            val firstSource = sourceList[0]
            location.source = firstSource
        }
    }

    private fun extractFirstReviewSummary(reviewSummaryListJson: JsonElement, location: Location) {
        val reviewSummaryList = mGson?.fromJson<List<ReviewSummary>>(
            reviewSummaryListJson,
            mReviewSummaryListTypeToken.type
        ).orEmpty()
        if (!ListUtils.isEmpty(reviewSummaryList)) {
            val firstReviewSummary = reviewSummaryList[0]
            location.averageRating = firstReviewSummary.averageRating
            location.ratingCount = firstReviewSummary.reviewCount
            location.ratingImageUrl = firstReviewSummary.ratingImageURL
        }
    }

    companion object {
        private const val KEY_REVIEW_SUMMARIES = "reviewSummaries"
        private const val KEY_CLASS = "class"
        private const val KEY_SOURCES = "sources"
        private const val VALUE_CLASS_STOP_LOCATION = "StopLocation"
    }
}