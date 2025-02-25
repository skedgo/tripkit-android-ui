package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align.CENTER
import android.graphics.Paint.Style.FILL
import android.graphics.Paint.Style.STROKE
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import com.skedgo.tripkit.ui.R
import javax.inject.Inject

class VehicleMarkerIconCreator @Inject constructor(private val resources: Resources) {
    private val paint = Paint()
    private val borderPaint: Paint
    private val textPaint: Paint
    private val strokeWidth: Int

    init {
        paint.isAntiAlias = true
        paint.style = FILL

        borderPaint = Paint()
        borderPaint.color = Color.WHITE
        borderPaint.isAntiAlias = true
        borderPaint.style = STROKE
        strokeWidth = resources.getDimensionPixelSize(R.dimen.vehicleMarkerBorderWidth)
        borderPaint.strokeWidth = strokeWidth.toFloat()

        textPaint = Paint()
        textPaint.isAntiAlias = true
        textPaint.isDither = true
        textPaint.color = Color.WHITE
        textPaint.textSize =
            resources.getDimensionPixelSize(R.dimen.vehicleMarkerTextSize)
                .toFloat()
        textPaint.textAlign = CENTER
        textPaint.setTypeface(Typeface.DEFAULT_BOLD)
    }

    fun call(bearing: Int, color: Int, text: String): Bitmap {
        val height = resources.getDimensionPixelSize(R.dimen.vehicleMarkerHeight)
        val width = resources.getDimensionPixelSize(R.dimen.vehicleMarkerWidth)

        // Make it squared to increase touch target.
        val icon = Bitmap.createBitmap(height, height, ARGB_8888)
        val canvas = Canvas(icon)
        val center = height / 2f

        paint.color = color

        // Draw pyramid part.
        canvas.save()
        run {
            canvas.translate((height - width) / 2.0f, 0f)
            // Compute this to prevent the stroke from being cropped.
            // This is due to the fact that the paint draws the line in center mode.
            val halfStrokeWidth = strokeWidth / 2f
            val path = Path()
            path.moveTo(width / 2.0f, halfStrokeWidth)
            path.lineTo(width.toFloat(), height * 0.25f)
            path.lineTo(width.toFloat(), height - halfStrokeWidth)
            path.lineTo(0f, height - halfStrokeWidth)
            path.lineTo(0f, height * 0.25f)
            path.lineTo(width / 2.0f, halfStrokeWidth)
            path.close()

            canvas.drawPath(path, paint)
            canvas.drawPath(path, borderPaint)
        }
        canvas.restore()

        // Draw text.
        canvas.save()
        run {
            // Rotate to draw text the correct direction.
            canvas.rotate(-90.0f, center, center)

            // Make sure we always show the text the right way up.
            val shouldFlip = bearing > 180.0f && bearing < 360.0f
            if (shouldFlip) {
                canvas.rotate(180.0f, center, center)
            }

            // Following technique was taken from https://chris.banes.me/2014/03/27/measuring-text/.
            val textBounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textWidth = textBounds.width()
            val textHeight = textBounds.height()

            val arrowPadding = height * 0.25f
            canvas.drawText(
                text,
                center - textWidth * 0.5f + arrowPadding,
                center + (textHeight / 2f),
                textPaint
            )
        }
        canvas.restore()
        return icon
    }
}