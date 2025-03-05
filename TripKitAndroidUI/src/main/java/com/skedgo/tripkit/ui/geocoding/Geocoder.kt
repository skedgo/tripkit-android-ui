package com.skedgo.tripkit.ui.geocoding

import androidx.core.util.Pair
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.util.Gsons.createForLowercaseEnum
import com.skedgo.tripkit.ui.utils.HttpUtils
import java.io.IOException

open class Geocoder {
    open var nearLatitude: Double = Double.MAX_VALUE
        protected set
    open var nearLongitude: Double = Double.MAX_VALUE
        protected set
    @JvmField
    var mGson: Gson = GsonBuilder()
        .registerTypeAdapter(Location::class.java, GeocodeResultAdapter(createForLowercaseEnum()))
        .create()
    open val serviceUrl: String? = null
    private val mAllowGoogle = false

    open fun setNearLatitude(nearLatitude: Double): Geocoder {
        this.nearLatitude = nearLatitude
        return this
    }

    open fun setNearLongitude(nearLongitude: Double): Geocoder {
        this.nearLongitude = nearLongitude
        return this
    }

    @Throws(IOException::class)
    open fun query(query: String): List<Location>? {
        val response = HttpUtils.get(serviceUrl + GEOCODE_METHOD, asParams(query))
        val geocodeResponse = mGson.fromJson(response, GeocodeResponse::class.java)
        return geocodeResponse.choiceList
    }

    protected fun asParams(query: String): List<Pair<String, Any>> {
        val params: MutableList<Pair<String, Any>> = ArrayList()
        params.add(Pair(PARAM_QUERY, query))
        params.add(Pair(PARAM_ALLOW_YELP, true))
        params.add(Pair(PARAM_ALLOW_GOOGLE, mAllowGoogle))
        if (java.lang.Double.compare(nearLatitude, Double.MAX_VALUE) != 0
            && java.lang.Double.compare(nearLongitude, Double.MAX_VALUE) != 0
        ) {
            val nearAddress = "(" + nearLatitude + "," + nearLongitude + ")"
            params.add(Pair(PARAM_NEAR, nearAddress))
        }
        return params
    }

    companion object {
        protected const val PARAM_QUERY: String = "q"
        protected const val PARAM_NEAR: String = "near"
        protected const val PARAM_ALLOW_YELP: String = "allowYelp"
        protected const val PARAM_ALLOW_GOOGLE: String = "allowGoogle" //allowGoogle=false
        @JvmStatic
        protected val GEOCODE_METHOD: String = "/geocode.json"
    }
}
