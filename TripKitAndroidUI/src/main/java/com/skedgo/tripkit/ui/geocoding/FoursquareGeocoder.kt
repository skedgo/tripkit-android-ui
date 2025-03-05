package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.GCFoursquareResult
import com.skedgo.tripkit.common.model.location.Location
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Use OkHttp for requests.
class FoursquareGeocoder(input: String, nearbyLat: Double, nearbyLon: Double) {
    private val input: String
    private var nearbyLat = Double.MAX_VALUE
    private var nearbyLon = Double.MAX_VALUE

    init {
        this.nearbyLat = nearbyLat
        this.nearbyLon = nearbyLon
        this.input = input
    }

    val locationsFromFoursquare: List<Location>
        get() {
            val gcList = fromFoursquare
            val locations: MutableList<Location> = ArrayList(gcList.size)

            for (gcLocation in gcList) {
                locations.add(gcLocation!!.getPlace().location)
            }

            return locations
        }

    val fromFoursquare: List<FoursquareResultLocationAdapter?>
        get() {
            var results = ArrayList<FoursquareResultLocationAdapter?>()

            var connection: HttpURLConnection? = null
            var stream: InputStream? = null
            val jsonResults = StringBuilder()
            var errorAtConnection = false
            try {
                val url = foursquareUrl
                connection = url.openConnection() as HttpURLConnection

                stream = connection.inputStream
                val `in` = InputStreamReader(stream)

                // Load the results into a StringBuilder
                var read: Int
                val buff = CharArray(1024)
                while ((`in`.read(buff).also { read = it }) != -1) {
                    jsonResults.append(buff, 0, read)
                }
            } catch (e: Exception) {
                Timber.e(e)
                errorAtConnection = true
            } finally {
                connection?.disconnect()
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e: IOException) {
                        Timber.e(e)
                    }
                }
            }

            if (!errorAtConnection) {
                try {
                    // Create a JSON object hierarchy from the results
                    val jsonObj = JSONObject(jsonResults.toString())
                    val responseJson = jsonObj.getJSONObject("response")
                    val venuesJson = responseJson.getJSONArray("venues")

                    results = getFoursquareLocations(venuesJson)
                } catch (e: JSONException) {
                    Timber.e(e)
                }
            }
            return results
        }

    @Throws(JSONException::class)
    private fun getFoursquareLocations(venuesJson: JSONArray): ArrayList<FoursquareResultLocationAdapter?> {
        val locations = ArrayList<FoursquareResultLocationAdapter?>(venuesJson.length())
        for (i in 0 until venuesJson.length()) {
            val venueJson = venuesJson.getJSONObject(i)
            locations.add(createGCFoursquareResult(venueJson))
        }
        return locations
    }

    private fun createGCFoursquareResult(choice: JSONObject): FoursquareResultLocationAdapter? {
        try {
            val name = choice.getString("name")
            val location = choice.getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            val verified = choice.optBoolean("verified", false)

            val jsonCategories = choice.getJSONArray("categories")
            val categories: MutableList<String> = ArrayList()
            for (i in 0 until jsonCategories.length()) {
                val category = jsonCategories[i] as JSONObject
                categories.add(category.getString("shortName"))
            }

            val loc = Location(lat, lng)
            val address = choice.optString("address")

            loc.address = address
            loc.name = name
            loc.source = Location.FOURSQUARE

            val result = GCFoursquareResult(name, lat, lng, verified, categories)

            return FoursquareResultLocationAdapter(loc, result)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    @get:Throws(
        UnsupportedEncodingException::class,
        MalformedURLException::class
    )
    private val foursquareUrl: URL
        get() {
            val date = Date()
            val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
            val dateString = simpleDateFormat.format(date)
            val sb = StringBuilder("https://api.foursquare.com/v2/venues/search?ll=")
            if (nearbyLat != Double.MAX_VALUE || nearbyLon != Double.MAX_VALUE) {
                sb.append(nearbyLat).append(",").append(nearbyLon)
            } else {
                sb.append("-33.892387,151.187315") //sydney
            }
            sb.append("&query=" + URLEncoder.encode(input, "utf8"))
            sb.append(("&client_id=" + FOURSQUARE_CLIENT_ID))
            sb.append(("&client_secret=" + FOURSQUARE_CLIENT_SECRET))
            sb.append("&v=").append(dateString)
            return URL(sb.toString())
        }

    companion object {
        private const val FOURSQUARE_CLIENT_ID = "0QZSSYNBJL1SC3KG45OIO41PIMQIHEB10V2HBSBMUGLZMVYZ"
        private const val FOURSQUARE_CLIENT_SECRET =
            "NZJOMT2ULWYDOJCN0UHUSIXWALP2AQ3NI4WXY5X0LKEY5HNR"
    }
}
