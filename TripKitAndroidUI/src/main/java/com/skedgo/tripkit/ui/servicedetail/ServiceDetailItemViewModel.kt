package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.NinePatchDrawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.common.model.ServiceStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.utils.TapAction
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject


class ServiceDetailItemViewModel @Inject constructor(val getStopTimeDisplayText: GetStopTimeDisplayText) :
    RxViewModel() {
    enum class LineDirection {
        START, MIDDLE, END
    }

    var stop: ServiceStop? = null
    val scheduledTime = ObservableField<String>()
    val scheduledTimeTextColor = ObservableInt()
    var lineColor = 0
    val lineDrawable = ObservableField<NinePatchDrawable>()
    val stopName = ObservableField<String>()
    val stopNameColor = ObservableInt()
    val onItemClick = TapAction.create { stop }

    fun setDrawable(context: Context, direction: LineDirection) {
        when (direction) {
            LineDirection.START -> lineDrawable.set(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.service_line_start
                ) as NinePatchDrawable
            )
            LineDirection.MIDDLE -> lineDrawable.set(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.service_line_middle
                ) as NinePatchDrawable
            )
            LineDirection.END -> lineDrawable.set(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.service_line_end
                ) as NinePatchDrawable
            )
        }

        lineDrawable.get()?.setColorFilter(lineColor, PorterDuff.Mode.SRC_ATOP)
    }

    fun setStop(context: Context, stop: ServiceStop, _lineColor: Int, travelled: Boolean) {
        this.stop = stop
        lineColor = _lineColor
        stopName.set(stop.name)
        if (travelled) {
            scheduledTimeTextColor.set(ContextCompat.getColor(context, R.color.black2))
            stopNameColor.set(ContextCompat.getColor(context, R.color.black2))
        } else {
            scheduledTimeTextColor.set(ContextCompat.getColor(context, R.color.black1))
            stopNameColor.set(ContextCompat.getColor(context, R.color.black))
        }
        scheduledTimeTextColor.set(ContextCompat.getColor(context, R.color.black1))
        getStopTimeDisplayText.execute(stop)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ text ->
                scheduledTime.set(text)
            }, { Timber.e(it) }).autoClear()
    }

}