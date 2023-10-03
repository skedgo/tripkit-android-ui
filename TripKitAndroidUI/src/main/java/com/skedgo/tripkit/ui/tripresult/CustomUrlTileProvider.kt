package com.skedgo.tripkit.ui.tripresult

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream

class CustomUrlTileProvider(private val urls: List<String>) : TileProvider {

    private val bitmapCache: MutableMap<String, Bitmap?> = HashMap()

    var mapLoaded: () -> Unit = {}

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {

        val tileKey = getTileKey(x, y, zoom)

        // Check if the bitmap is cached
        val cachedBitmap = bitmapCache[tileKey]
        if (cachedBitmap != null) {
            return Tile(256, 256, bitmapToBytes(cachedBitmap))
        }

        for (url in urls) {
            val tileUrl = getTileUrl(url, x, y, zoom)
            Log.e("CustomUrlTileProvider", "tileUrl: $tileUrl")
            val bitmap: Bitmap? = loadTileImage(tileUrl)

            if (bitmap != null) {
                bitmapCache[tileKey] = bitmap
                mapLoaded.invoke()
                return Tile(256, 256, bitmapToBytes(bitmap))
            }
        }

        // Handle the case where all URLs failed to load
        return Tile(256, 256, ByteArray(0))
    }

    private fun getTileKey(x: Int, y: Int, zoom: Int): String {
        // Generate a unique key based on the tile's x, y, and zoom
        return "$zoom/$x/$y"
    }

    private fun getTileUrl(url: String, x: Int, y: Int, zoom: Int): String {
        // Construct the URL for the tile based on x, y, and zoom level
        // You may need to customize this based on your tile server's URL structure
        return url.replace("{x}", x.toString())
            .replace("{y}", y.toString())
            .replace("{z}", zoom.toString())
    }

    private fun loadTileImage(tileUrl: String): Bitmap? {
        try {
            val inputStream = java.net.URL(tileUrl).openStream()
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            // Handle the error (e.g., network error, invalid URL)
            Log.e("CustomUrlTileProvider", "error: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun clear() {
        bitmapCache.clear()
    }
}
