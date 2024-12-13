package com.skedgo.tripkit.ui.geocoding

import androidx.core.util.Pair
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.utils.HttpUtils
import java.io.IOException
import javax.inject.Inject

class GeocoderLive @Inject constructor() : RegionalGeocoder() {
    companion object {
        private const val PARAM_AUTOCOMPLETE = "a"
        private const val ON_VALUE = 1
    }

    override var nearLatitude: Double = Double.MAX_VALUE

    override var nearLongitude: Double = Double.MAX_VALUE

    override fun setNearLatitude(nearLatitude: Double): GeocoderLive {
        this.nearLatitude = nearLatitude
        return this
    }

    override fun setNearLongitude(nearLongitude: Double): GeocoderLive {
        this.nearLongitude = nearLongitude
        return this
    }

    @Throws(IOException::class)
    override fun query(query: String): List<Location>? {
        val params = asParams(query).toMutableList()
        params.add(Pair(PARAM_AUTOCOMPLETE, ON_VALUE))

        val url = "$serviceUrl${GEOCODE_METHOD}"
        val response = HttpUtils.get(url, params)
        val geocodeResponse = mGson.fromJson(response, GeocodeResponse::class.java)
        return geocodeResponse?.choiceList
    }
}
