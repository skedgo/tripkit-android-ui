package com.skedgo.tripkit.ui.routeinput

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import javax.inject.Inject

/**
 * A widget for display start and destination locations, as well as departure/arrival times and a "Route" button.
 *
 */
class RouteInputView : CardView, View.OnClickListener {
    /**
     * Interface definition for a callback that is called when one of several widgets are clicked.
     */
    interface OnRouteWidgetClickedListener {
        enum class Widget {
            /**
             * The "Start" EditText.
             */
            START,
            /**
             * The "Destination" EditText.
             */
            DESTINATION,
            /**
             * The "Swap Start and Destination" button.
             */
            SWAPPED,
            /**
             * The "Set Time" button.
             */
            TIME,
            /**
             * The "Route" button.
             */
            ROUTE
        }

        /**
         * Called when the button is clicked;.
         *
         * @param button The {@link #WhichButton} button} which was clicked.
         */
        fun widgetClicked(button: Widget)
    }

    private var mRouteWidgetClickedListener: OnRouteWidgetClickedListener? = null

    /**
     * Register a callback to be invoked when a widget is clicked.
     *
     * @param callback The callback that will be run.
     */
    fun setOnRouteWidgetClickedListener(callback: OnRouteWidgetClickedListener) {
        this.mRouteWidgetClickedListener = callback
    }
    fun setOnRouteWidgetClickedListener(listener:(OnRouteWidgetClickedListener.Widget) -> Unit) {
        this.mRouteWidgetClickedListener = object: OnRouteWidgetClickedListener {
            override fun widgetClicked(button: OnRouteWidgetClickedListener.Widget) {
                listener(button)
            }
        }
    }

    private var startEdit: EditText?
    private var destEdit: EditText?

    /**
     * @suppress
     */
    @Inject
    lateinit var viewModel: RouteInputViewModel

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        TripKitUI.getInstance().routeInputViewComponent().inject(this)

        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.route_input, this, true)

        startEdit = findViewById<EditText>(R.id.startEdit)
        startEdit?.setOnClickListener(this)
        destEdit = findViewById<EditText>(R.id.destEdit)
        destEdit?.setOnClickListener(this)

        val swapButton = findViewById<ImageButton>(R.id.swap)
        swapButton?.setOnClickListener(this)
        val timeButton = findViewById<MaterialButton>(R.id.timeButton)
        timeButton?.setOnClickListener(this)
        val routeButton = findViewById<MaterialButton>(R.id.routeButton)
        routeButton?.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        view?.let {
            when (it.id) {
                R.id.startEdit -> mRouteWidgetClickedListener?.widgetClicked(OnRouteWidgetClickedListener.Widget.START)
                R.id.destEdit -> mRouteWidgetClickedListener?.widgetClicked(OnRouteWidgetClickedListener.Widget.DESTINATION)
                R.id.timeButton -> mRouteWidgetClickedListener?.widgetClicked(OnRouteWidgetClickedListener.Widget.TIME)
                R.id.routeButton -> mRouteWidgetClickedListener?.widgetClicked(OnRouteWidgetClickedListener.Widget.ROUTE)
                R.id.swap -> {
                    val startText = startEdit?.text.toString()
                    val destText = destEdit?.text.toString()
                    startEdit?.setText(destText)
                    destEdit?.setText(startText)
                    mRouteWidgetClickedListener?.widgetClicked(OnRouteWidgetClickedListener.Widget.SWAPPED)
                }
                else -> {}
            }
        }
    }

    /**
     * Sets the displayed text in the Start field.
     *
     * @param text The text to display, which will most likely be the name of a location.
     */
    fun setStartText(text: String) {
        startEdit?.setText(text)
    }

    /**
     * Sets the displayed text in the Destination field.
     *
     * @param text The text to display, which will most likely be the name of a location.
     */
    fun setDestinationText(text: String) {
        destEdit?.setText(text)
    }

    /**
     * Sets the displayed text on the Time button.
     *
     * @param text The text to display
     */
    fun setTimeButton(text: String) {
        val timeButton = findViewById<MaterialButton>(R.id.timeButton)
        timeButton.text = text
    }

}
