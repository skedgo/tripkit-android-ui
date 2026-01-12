package com.skedgo.tripkit.ui.dialog

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.text.format.Time
import android.view.View
import android.view.View.OnClickListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.ui.R
import kankan.wheel.widget.WheelView
import kankan.wheel.widget.adapters.ArrayWheelAdapter
import kankan.wheel.widget.adapters.DaysAdapter
import kankan.wheel.widget.adapters.NumericWheelAdapter
import java.lang.reflect.Field
import java.util.Calendar

class TimeDatePickerFragment : DialogFragment(), OnClickListener {
    var timeRelay: PublishRelay<Long> = PublishRelay.create()
    private var mCalendar: Calendar? = null
    private var mTitle: String? = null
    private var mInitiatorId: String? = null

    /**
     * Should show date and time or not.
     */
    private val mShouldShowTime = true
    private var mHoursAdapter: NumericWheelAdapter? = null
    private var mMinutesAdapter: NumericWheelAdapter? = null
    private var mAmPmAdapter: ArrayWheelAdapter<String>? = null
    private var mDaysAdapter: DaysAdapter? = null
    private var mHoursView: WheelView? = null
    private var mMinutesView: WheelView? = null
    private var mAmPmView: WheelView? = null
    private var mDaysView: WheelView? = null
    private var mTimeType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mTimeType = arguments?.getInt(ARG_TIME_TYPE) ?: 0
        if (mTimeType == TimeDatePickedEvent.TIME_TYPE_BEGIN) {
            mTitle = getString(R.string.set_start_time)
        } else if (mTimeType == TimeDatePickedEvent.TIME_TYPE_END) {
            mTitle = getString(R.string.set_end_time)
        } else {
            mTitle = arguments?.getString(ARG_TITLE)
            if (mTitle == null) {
                mTitle = getString(R.string.set_time)
            }
        }


        val time = arguments?.getLong(ARG_INITIAL_TIME, System.currentTimeMillis())
        timeRelay.accept(time)
        mInitiatorId = arguments?.getString(ARG_INITIATOR_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = FlatAlertDialogBuilder(requireActivity())
        val dialog = dialogBuilder
            .setTitle(mTitle)
            .setContentView(R.layout.dialog_time_date_picker)
            .setPositiveButton(R.string.done) { dialog, buttonType ->
                this@TimeDatePickerFragment.onClick(
                    null
                )
            }
            .setNegativeButton(R.string.cancel, { dialog, buttonType -> dismiss() }, null)
            .create()

        val view = dialogBuilder.contentView

        //set hour view
        mHoursView = view.findViewById<View>(R.id.hoursView) as WheelView
        mHoursAdapter = NumericWheelAdapter(this.activity, 1, 12, "%02d")
        mHoursAdapter!!.itemResource = R.layout.v4_view_wheel_time
        mHoursAdapter!!.itemTextResource = R.id.text
        mHoursView!!.viewAdapter = mHoursAdapter
        mHoursView!!.isCyclic = true

        //set min view
        mMinutesView = view.findViewById<View>(R.id.minutesView) as WheelView
        mMinutesAdapter = NumericWheelAdapter(this.activity, 0, 59, "%02d")
        mMinutesAdapter!!.itemResource = R.layout.v4_view_wheel_time
        mMinutesAdapter!!.itemTextResource = R.id.text
        mMinutesView!!.viewAdapter = mMinutesAdapter
        mMinutesView!!.isCyclic = true

        //set am, pm view
        mAmPmView = view.findViewById<View>(R.id.amPmView) as WheelView
        mAmPmAdapter = ArrayWheelAdapter(this.activity, arrayOf("AM", "PM"))
        mAmPmAdapter!!.itemResource = R.layout.v4_view_wheel_time
        mAmPmAdapter!!.itemTextResource = R.id.text
        mAmPmView!!.viewAdapter = mAmPmAdapter

        if (!mShouldShowTime) {
            mHoursView!!.visibility = View.GONE
            mMinutesView!!.visibility = View.GONE
            mAmPmView!!.visibility = View.GONE
        }

        val initialTimeInMillis = arguments?.getLong("initialTimeInMillis")  ?: 0
        mCalendar = Calendar.getInstance()
        mCalendar!!.timeInMillis = initialTimeInMillis

        // set time
        //index count from 0, but hour count is face-value
        mHoursView?.currentItem = mCalendar!!.get(Calendar.HOUR) - 1
        mMinutesView?.currentItem = mCalendar!!.get(Calendar.MINUTE)
        mAmPmView?.currentItem = mCalendar!!.get(Calendar.AM_PM)

        mDaysView = view.findViewById<View>(R.id.daysView) as WheelView
        val dayRange = 60
        mDaysAdapter = DaysAdapter(this.activity, mCalendar, dayRange)
        mDaysAdapter?.apply {
            itemResource = R.layout.v4_view_wheel_time
            itemTextResource = R.id.text
        }
        mDaysView?.viewAdapter = mDaysAdapter
        mDaysView?.currentItem = (dayRange + 1) / 2

        // Set shadow drawables for dark mode support
        setWheelViewShadows(mDaysView)
        setWheelViewShadows(mHoursView)
        setWheelViewShadows(mMinutesView)
        setWheelViewShadows(mAmPmView)

        // Set center drawable and divider lines for dark mode support
        setWheelViewCenterDrawable(mDaysView)
        setWheelViewCenterDrawable(mHoursView)
        setWheelViewCenterDrawable(mMinutesView)
        setWheelViewCenterDrawable(mAmPmView)

        return dialog
    }

    override fun onClick(view: View?) {
        var hour = 0
        var mins = 0
        var amPm = "AM"

        if (mShouldShowTime) {
            //what index does it return if it is cyclic => answer: correct index
            val hourIndex = mHoursView!!.currentItem
            val minuteIndex = mMinutesView!!.currentItem
            val apmIndex = mAmPmView!!.currentItem

            hour = mHoursAdapter?.getItemValue(hourIndex) ?: 0
            mins = mMinutesAdapter?.getItemValue(minuteIndex) ?: 0
            amPm = mAmPmAdapter?.getItemText(apmIndex) as String
        }

        val dayIndex = mDaysView!!.currentItem
        val dayRepresentedByMillis = mDaysAdapter!!.getItemValue(dayIndex)

        //note: the day/time wheel does not imply timezone. Tz must be explicitly set.
        val time = Time()
        time.set(dayRepresentedByMillis)
        time.normalize(false)

        //12=>0, 1=>1, .etc
        val hourCorrectedBy0 = hour % 12
        if (amPm.equals("AM", ignoreCase = true)) {
            time.hour = hourCorrectedBy0
        } else {
            time.hour = hourCorrectedBy0 + 12
        }

        time.minute = mins

        //normalize after setting the hour and min, if Tz is set, remember to normalize
        time.normalize(false)
        timeRelay.accept(time.toMillis(false))
        dismiss()
    }

    /**
     * Sets the top and bottom shadow drawables for a WheelView to support dark mode.
     * Uses reflection to access private fields in the WheelView library.
     */
    private fun setWheelViewShadows(wheelView: WheelView?) {
        if (wheelView == null) return

        try {
            val context = requireContext()
            val topShadow = ContextCompat.getDrawable(context, R.drawable.top_shadow)
            val bottomShadow = ContextCompat.getDrawable(context, R.drawable.bottom_shadow)

            if (topShadow == null || bottomShadow == null) return

            // Get shadow fields directly
            val topShadowField = wheelView.javaClass.getDeclaredField(WHEEL_VIEW_FIELD_TOP_SHADOW)
            topShadowField.isAccessible = true
            
            val bottomShadowField = wheelView.javaClass.getDeclaredField(WHEEL_VIEW_FIELD_BOTTOM_SHADOW)
            bottomShadowField.isAccessible = true

            // Set the shadow drawables
            topShadowField.set(wheelView, topShadow)
            bottomShadowField.set(wheelView, bottomShadow)

            // Invalidate to refresh the view
            wheelView.invalidate()
        } catch (e: Exception) {
            // Silently fail if reflection doesn't work (library might have changed)
            e.printStackTrace()
        }
    }

    /**
     * Sets the divider lines for a WheelView to support dark mode.
     * Uses reflection to access private fields in the WheelView library.
     */
    private fun setWheelViewCenterDrawable(wheelView: WheelView?) {
        if (wheelView == null) return

        try {
            val context = requireContext()
            // Use @color/black which inverts to white in dark mode
            val dividerColor = ContextCompat.getColor(context, R.color.black)
            val lineDrawable = ColorDrawable(dividerColor)

            // Get line fields directly
            val topLineField = wheelView.javaClass.getDeclaredField(WHEEL_VIEW_FIELD_TOP_LINE)
            topLineField.isAccessible = true
            
            val bottomLineField = wheelView.javaClass.getDeclaredField(WHEEL_VIEW_FIELD_BOTTOM_LINE)
            bottomLineField.isAccessible = true

            // Set the drawables
            topLineField.set(wheelView, lineDrawable)
            bottomLineField.set(wheelView, lineDrawable)

            // Invalidate to refresh the view
            wheelView.invalidate()
        } catch (e: Exception) {
            // Silently fail if reflection doesn't work (library might have changed)
            e.printStackTrace()
        }
    }

    companion object {
        // WheelView reflection field names
        private const val WHEEL_VIEW_FIELD_TOP_SHADOW = "topShadow"
        private const val WHEEL_VIEW_FIELD_BOTTOM_SHADOW = "bottomShadow"
        private const val WHEEL_VIEW_FIELD_TOP_LINE = "topLine"
        private const val WHEEL_VIEW_FIELD_BOTTOM_LINE = "bottomLine"
        private const val ARG_TIME_TYPE = "timeType"
        private const val ARG_INITIATOR_ID = "initiatorId"
        private const val ARG_INITIAL_TIME = "initialTimeInMillis"
        private const val ARG_TITLE = "title"
        @JvmOverloads
        fun newInstance(
            timeType: Int,
            initiatorId: String? = null,
            title: String? = null,
            initialTimeInMillis: Long = System.currentTimeMillis()
        ): TimeDatePickerFragment {
            val args = Bundle()
            args.putString(ARG_INITIATOR_ID, initiatorId)
            args.putInt(ARG_TIME_TYPE, timeType)
            args.putLong(ARG_INITIAL_TIME, initialTimeInMillis)
            if (!TextUtils.isEmpty(title)) {
                args.putString(ARG_TITLE, title)
            }

            val fragment = TimeDatePickerFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(title: String?): TimeDatePickerFragment {
            return newInstance(
                TimeDatePickedEvent.TIME_TYPE_OTHER,
                null,
                title,
                System.currentTimeMillis()
            )
        }
    }
}