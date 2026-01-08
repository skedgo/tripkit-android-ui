package com.skedgo.tripkit.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Pair
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.isDarkMode
import kotlin.math.abs

class BearingMarkerIconBuilder(
    private val mContext: Context,
    private val mTimeLabelMaker: TimeLabelMaker?
) {
    private var mHasBearing = false
    private var mBearing = 0
    private var mHasBearingVehicleIcon = false
    private var mBaseIconResourceId = 0
    private var mPointerIconResourceId = 0
    private var mHasTime = false
    private var mMillis: Long = 0
    private val mRotationPaint = Paint()
    private var vehicleIconRes: Drawable? = null
    private var vehicleIconScale = 1f
    private var mTimezone: String? = null

    init {
        mRotationPaint.isAntiAlias = true
        mRotationPaint.isFilterBitmap = true
    }

    fun hasBearing(hasBearing: Boolean): BearingMarkerIconBuilder {
        mHasBearing = hasBearing
        return this
    }

    fun bearing(bearing: Int): BearingMarkerIconBuilder {
        mBearing = bearing
        return this
    }

    /**
     * @param vehicleIconRes Must be an instance of [BitmapDrawable].
     */
    fun vehicleIcon(vehicleIconRes: Drawable?): BearingMarkerIconBuilder {
        this.vehicleIconRes = vehicleIconRes
        return this
    }

    fun vehicleIconScale(vehicleIconScale: Float): BearingMarkerIconBuilder {
        this.vehicleIconScale = vehicleIconScale
        return this
    }

    fun baseIcon(@DrawableRes baseIconResourceId: Int): BearingMarkerIconBuilder {
        mBaseIconResourceId = baseIconResourceId
        return this
    }

    fun pointerIcon(@DrawableRes pointerIconResourceId: Int): BearingMarkerIconBuilder {
        mPointerIconResourceId = pointerIconResourceId
        return this
    }

    fun hasTime(hasTime: Boolean): BearingMarkerIconBuilder {
        mHasTime = hasTime
        return this
    }

    fun time(millis: Long, timeZone: String?): BearingMarkerIconBuilder {
        mMillis = millis
        mTimezone = timeZone
        return this
    }

    fun hasBearingVehicleIcon(hasBearingVehicleIcon: Boolean): BearingMarkerIconBuilder {
        mHasBearingVehicleIcon = hasBearingVehicleIcon
        return this
    }

    fun build(): Pair<Bitmap, Float> {
        val vehiclePointerBitmap = createVehiclePointerBitmap()
        val vehiclePointerPinBitmap = createVehiclePointerPinBitmap(vehiclePointerBitmap)
        vehiclePointerBitmap.recycle()

        if (mHasTime && mTimeLabelMaker != null) {
            val markerIcon = plusTimeLabel(vehiclePointerPinBitmap)
            vehiclePointerPinBitmap.recycle()
            return markerIcon
        } else {
            return Pair(vehiclePointerPinBitmap, 0.5f)
        }
    }

    private fun plusTimeLabel(vehiclePointerPinBitmap: Bitmap): Pair<Bitmap, Float> {
        val timeLabelBitmap = mTimeLabelMaker!!.create(mMillis, mTimezone!!)
        val finalBitmap = Bitmap.createBitmap(
            vehiclePointerPinBitmap.width + timeLabelBitmap.width,
            vehiclePointerPinBitmap.height,
            ARGB_8888
        )

        val canvas = Canvas(finalBitmap)
        val rotateAngle = convertToCanvasAxes(mBearing)
        val bearingToWesternSide = isBearingToWesternSide(rotateAngle)

        val offset = mContext.resources.getDimensionPixelSize(R.dimen.v4_content_padding)
        val timeLabelLeft = if (bearingToWesternSide
        ) vehiclePointerPinBitmap.width - offset
        else offset
        val timeLabelTop = (vehiclePointerPinBitmap.width - timeLabelBitmap.height) / 2f

        val timeLabelBackgroundDrawable =
            ContextCompat.getDrawable(mContext, R.drawable.v4_shape_map_time_label)
        if (timeLabelBackgroundDrawable != null) {
            canvas.save()
            run {
                val timeLabelBackgroundLeft = if (bearingToWesternSide
                ) vehiclePointerPinBitmap.width / 2f
                else offset.toFloat()
                canvas.translate(timeLabelBackgroundLeft, timeLabelTop)

                timeLabelBackgroundDrawable.setBounds(
                    0, 0,
                    timeLabelBitmap.width + vehiclePointerPinBitmap.width / 2 - offset,
                    timeLabelBitmap.height
                )
                timeLabelBackgroundDrawable.draw(canvas)
            }
            canvas.restore()
        }

        canvas.drawBitmap(timeLabelBitmap, timeLabelLeft.toFloat(), timeLabelTop, null)
        timeLabelBitmap.recycle()

        val vehiclePointerPinBitmapLeft = if (!bearingToWesternSide
        ) finalBitmap.width - vehiclePointerPinBitmap.width
        else 0
        canvas.drawBitmap(vehiclePointerPinBitmap, vehiclePointerPinBitmapLeft.toFloat(), 0f, null)

        val anchor = vehiclePointerPinBitmap.width / (2f * finalBitmap.width)
        val anchorU = if (bearingToWesternSide) anchor else 1.0f - anchor
        return Pair(finalBitmap, anchorU)
    }

    private fun createVehiclePointerPinBitmap(vehiclePointerBitmap: Bitmap): Bitmap {
        // Padding between the VehiclePointer and the PinBase
        val padding = -mContext.resources.getDimensionPixelSize(R.dimen.v4_base_pointer_padding) * 2
        val baseBitmap = BitmapFactory.decodeResource(mContext.resources, mBaseIconResourceId)
        val vehiclePointerPinBitmap = Bitmap.createBitmap(
            vehiclePointerBitmap.width,
            vehiclePointerBitmap.height + padding + baseBitmap.height,
            ARGB_8888
        )

        val canvas = Canvas(vehiclePointerPinBitmap)

        // Apply dark mode tinting to base (make it darker for dark mode)
        val basePaint = if (mContext.resources.isDarkMode()) {
            Paint().apply {
                colorFilter = createDarkenColorFilter()
                isAntiAlias = true
                isFilterBitmap = true
            }
        } else {
            null
        }

        // Locate the base
        val baseLeft = (vehiclePointerBitmap.width - baseBitmap.width) / 2
        val baseTop = vehiclePointerBitmap.height + padding
        canvas.drawBitmap(baseBitmap, baseLeft.toFloat(), baseTop.toFloat(), basePaint)
        canvas.drawBitmap(vehiclePointerBitmap, 0f, 0f, null)

        return vehiclePointerPinBitmap
    }

    private fun createVehiclePointerBitmap(): Bitmap {
        val pointerBitmap = BitmapFactory.decodeResource(mContext.resources, mPointerIconResourceId)
        val vehiclePointerBitmap = Bitmap.createBitmap(
            pointerBitmap.width,
            pointerBitmap.height,
            ARGB_8888
        )

        val canvas = Canvas(vehiclePointerBitmap)

        // Apply dark mode tinting to pointer (make it darker for dark mode)
        val paint = if (mContext.resources.isDarkMode()) {
            Paint().apply {
                colorFilter = createDarkenColorFilter()
                isAntiAlias = true
                isFilterBitmap = true
            }
        } else {
            null
        }

        val rotateAngle = convertToCanvasAxes(mBearing)
        if (mHasBearing) {
            canvas.save()

            // Rotate the Pointer bitmap to reflect bearing
            canvas.rotate(
                rotateAngle.toFloat(),
                (vehiclePointerBitmap.width / 2).toFloat(),
                (vehiclePointerBitmap.height / 2).toFloat()
            )
            canvas.drawBitmap(pointerBitmap, 0f, 0f, paint ?: mRotationPaint)

            canvas.restore()
        } else {
            canvas.drawBitmap(pointerBitmap, 0f, 0f, paint)
        }

        pointerBitmap.recycle()

        // Place the Vehicle bitmap onto the Pointer bitmap
        canvas.save()

        val vehicleBitmap: Bitmap = convertDrawableToBitmap(vehicleIconRes)
        if (mHasBearing && mHasBearingVehicleIcon && isBearingToWesternSide(rotateAngle)) {
            // Flip drawing horizontally to reflect bearing: East -> West
            canvas.scale(-1.0f, 1.0f)
            canvas.translate(-vehiclePointerBitmap.width.toFloat(), 0f)
        }

        val vehicleBitmapLeft = (vehiclePointerBitmap.width - vehicleBitmap.width) / 2
        val vehicleBitmapTop = (vehiclePointerBitmap.height - vehicleBitmap.height) / 2
        canvas.drawBitmap(
            vehicleBitmap,
            vehicleBitmapLeft.toFloat(),
            vehicleBitmapTop.toFloat(),
            null
        )

        vehicleBitmap.recycle()

        canvas.restore()
        return vehiclePointerBitmap
    }

    private fun isBearingToWesternSide(rotateAngle: Int): Boolean {
        return rotateAngle >= 90 && rotateAngle <= 270
    }

    private fun convertToCanvasAxes(travelDirection: Int): Int {
        return 360 - correctTravelDirection(travelDirection)
    }

    /**
     * Recomputes travel direction against Pointer's default direction (90 in degrees)
     */
    private fun correctTravelDirection(travelDirection: Int): Int {
        return if (travelDirection >= 0 && travelDirection <= 90) {
            90 - travelDirection
        } else if (travelDirection > 90 && travelDirection <= 180) {
            abs((travelDirection - 360 - 90).toDouble()).toInt()
        } else if (travelDirection > 180 && travelDirection <= 360) {
            (abs((travelDirection - 360).toDouble()) + 90).toInt()
        } else {
            travelDirection
        }
    }

    private fun convertDrawableToBitmap(drawable: Drawable?): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.scale(
            vehicleIconScale,
            vehicleIconScale,
            (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat()
        )

        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Creates a ColorFilter that darkens white/light colors to medium-light gray for dark mode.
     * This makes white pin markers visible on dark map backgrounds.
     */
    private fun createDarkenColorFilter(): ColorMatrixColorFilter {
        // Color matrix that converts white (#FFFFFF) to medium-light gray (#999999 / 60% gray)
        // while preserving alpha channel
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                0.6f, 0f, 0f, 0f, 0f,   // Red channel: reduce to 60% (153/255)
                0f, 0.6f, 0f, 0f, 0f,   // Green channel: reduce to 60% (153/255)
                0f, 0f, 0.6f, 0f, 0f,   // Blue channel: reduce to 60% (153/255)
                0f, 0f, 0f, 1f, 0f      // Alpha channel: keep as-is
            )
        )
        return ColorMatrixColorFilter(colorMatrix)
    }
}