package com.skedgo.tripkit.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.io.ByteArrayOutputStream

@SuppressLint("StaticFieldLeak")
object MarkerIconManager {

    private lateinit var context: Context
    fun init(context: Context) {
        this.context = context
    }

    fun getMarkerBitmap(vectorResId: Int, customSize: Int? = null): BitmapDescriptor? =
        context.bitmapDescriptorFromVector(vectorResId, customSize ?: 40)

    private fun Context.bitmapDescriptorFromVector(
        vectorResId: Int,
        customSize: Int
    ): BitmapDescriptor? {
        return try {
            ContextCompat.getDrawable(this, vectorResId)?.run {
                setBounds(0, 0, customSize, customSize)

                // Resize the bitmap to an appropriate size
                val resizedBitmap = Bitmap.createScaledBitmap(
                    Bitmap.createBitmap(customSize, customSize, Bitmap.Config.ARGB_8888),
                    customSize,
                    customSize,
                    true
                )

                val canvas = Canvas(resizedBitmap)

                // Draw the resized bitmap
                draw(Canvas(resizedBitmap))

                // Compress the bitmap before creating BitmapDescriptor
                val stream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()

                BitmapDescriptorFactory.fromBitmap(resizedBitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}