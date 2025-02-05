package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import com.skedgo.tripkit.common.util.DateTimeFormats
import com.skedgo.tripkit.ui.R

class TimeLabelMaker(private val timeTextView: TextView) {
    private var canvas: Canvas? = null

    fun create(millis: Long, timeZone: String): Bitmap {
        val timeText = DateTimeFormats.printTime(
            timeTextView.context,
            millis,
            timeZone
        )
        timeTextView.text = timeText
        measureTimeTextViewSize()

        val timeLabelBitmap = Bitmap.createBitmap(
            timeTextView.measuredWidth,
            timeTextView.measuredHeight,
            ARGB_8888
        )

        val canvas = getCanvas(timeLabelBitmap)
        timeTextView.draw(canvas)

        return timeLabelBitmap
    }

    private fun measureTimeTextViewSize() {
        timeTextView.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        val widthSpec = MeasureSpec.makeMeasureSpec(
            LayoutParams.WRAP_CONTENT,
            MeasureSpec.UNSPECIFIED
        )
        val heightSpec = MeasureSpec.makeMeasureSpec(
            timeTextView.context.resources.getDimensionPixelSize(R.dimen.time_label_height),
            MeasureSpec.EXACTLY
        )
        timeTextView.measure(widthSpec, heightSpec)
        timeTextView.layout(0, 0, timeTextView.measuredWidth, timeTextView.measuredHeight)
    }

    private fun getCanvas(bitmap: Bitmap): Canvas {
        if (canvas == null) {
            canvas = Canvas()
        }

        canvas!!.setBitmap(bitmap)
        return canvas!!
    }
}