package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class OnSwipeTouchListener(context: Context, listener: SwipeGestureListener)
    : View.OnTouchListener {

    private var gestureDetector: GestureDetector = GestureDetector(context, GestureListener(listener))

    var touchCallback: (View?, MotionEvent?) -> Unit = { _, _ -> }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        touchCallback.invoke(p0, p1)
        return gestureDetector.onTouchEvent(p1)
    }


    class GestureListener(private val swipeGestureListener: SwipeGestureListener) :
            GestureDetector.SimpleOnGestureListener() {

        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {

            try {
                val distanceX = e2!!.x - (e1!!.x)
                val distanceY = e2.y - (e1.y)
                if (abs(distanceX) > abs(distanceY) && abs(distanceX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (distanceX > 0) {
                        swipeGestureListener.onSwipeLeft()
                    } else {
                        swipeGestureListener.onSwipeRight()
                    }
                    return true
                }
                return false
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }
    }

    interface SwipeGestureListener {
        fun onSwipeRight()
        fun onSwipeLeft()
    }
}