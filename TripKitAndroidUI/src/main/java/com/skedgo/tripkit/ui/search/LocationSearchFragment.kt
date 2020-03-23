package com.skedgo.tripkit.ui.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.AbstractTripKitFragment
import com.skedgo.tripkit.ui.databinding.LocationSearchBinding
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import javax.inject.Inject


/**
 * This is a self-contained location search component which merges search results from both SkedGo's search
 * results as well as Google Places.
 *
 * Use it with its Builder:
 *
 * ```
 *  LocationSearchFragment.Builder()
 *          .withBounds(mMap.projection.visibleRegion.latLngBounds)
 *          .near(mMap.cameraPosition.target)
 *          .withHint(getString(R.string.search))
 *          .allowCurrentLocation(true)
 *          .allowDropPin()
 *          .build()
 * ```
 */
class LocationSearchFragment : AbstractTripKitFragment() {
    /**
     * This callback will be invoked when a search result is clicked.
     */
    interface OnLocationSelectedListener {
        fun onLocationSelected(location: Location)
    }

    private var locationSelectedListener: OnLocationSelectedListener? = null
    fun setOnLocationSelectedListener(callback: OnLocationSelectedListener) {
        this.locationSelectedListener = callback
    }
    fun setOnLocationSelectedListener(listener:(Location) -> Unit) {
        this.locationSelectedListener = object: OnLocationSelectedListener {
            override fun onLocationSelected(location: Location) {
                listener(location)
            }

        }
    }

    /**
     * This callback will be invoked when the user chooses "Current Location"
     */
    interface OnCurrentLocationSelectedListener {
        fun onCurrentLocationSelected()
    }

    private var currentLocationSelectedListener: OnCurrentLocationSelectedListener? = null
    fun setOnCurrentLocationSelectedListener(callback: OnCurrentLocationSelectedListener) {
        this.currentLocationSelectedListener = callback
    }

    fun setOnCurrentLocationSelectedListener(listener:() -> Unit) {
        this.currentLocationSelectedListener = object: OnCurrentLocationSelectedListener {
            override fun onCurrentLocationSelected() {
                listener()
            }
        }
    }

    /**
     * This callback will be invoked when the user chooses "Choose on Map"
     */
    interface OnDropPinSelectedListener {
        fun onDropPinSelected()
    }

    private var dropPinSelectedListener: OnDropPinSelectedListener? = null
    fun setOnDropPinSelectedListener(callback: OnDropPinSelectedListener) {
        this.dropPinSelectedListener = callback
    }

    fun setOnDropPinSelectedListener(listener:() -> Unit) {
        this.dropPinSelectedListener = object: OnDropPinSelectedListener {
            override fun onDropPinSelected() {
                listener()
            }
        }
    }

    /**
     * @suppress
     */
    @Inject lateinit var viewModelFactory: LocationSearchViewModelFactory
    private lateinit var viewModel: LocationSearchViewModel
    /**
     * @suppress
     */
    @Inject lateinit var errorLogger: ErrorLogger
    private var searchView: SearchView? = null

    var locationSearchIconProvider: LocationSearchIconProvider? = null
    set(value) {
        field = value
        if (::viewModel.isInitialized) {
            viewModel.locationSearchIconProvider = value
        }
    }

    /**
     * @suppress
     */
    override fun onAttach(context: Context) {
        TripKitUI.getInstance().locationSearchComponent().inject(this);
        super.onAttach(context)
    }

    /**
     * @suppress
     */
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(LocationSearchViewModel::class.java);
        viewModel.locationSearchIconProvider = locationSearchIconProvider

        viewModel.locationChosen.compose(bindToLifecycle())
                .observeOn(mainThread())
                .subscribe({
                    locationSelectedListener?.onLocationSelected(it)
                }, errorLogger::trackError)
        viewModel.currentLocationChosen.compose(bindToLifecycle())
                .observeOn(mainThread())
                .subscribe({
                    currentLocationSelectedListener?.onCurrentLocationSelected()
                }, errorLogger::trackError)

        viewModel.pinDropChosen.compose(bindToLifecycle())
                .observeOn(mainThread())
                .subscribe({
                    dropPinSelectedListener?.onDropPinSelected()
                }, errorLogger::trackError)

        viewModel.dismiss
                .compose(bindToLifecycle())
                .observeOn(mainThread())
                .subscribe({
                    dismissKeyboard()

                    // FIXME: There's a case that the Search screen will not dismiss.
                    // Steps to reproduce:
                    // * Tap any Google search results.
                    // * Push the app into background immediately before
                    // the app manages to get the coordinates.
                    // * Then get back to the app. We'll still see the Search screen
                    // while it should have been dismissed.

                    val fragmentManager = fragmentManager
                    fragmentManager?.let {
                        if (!fragmentManager.isStateSaved) fragmentManager.popBackStackImmediate()
                    }
                }, errorLogger::trackError)
        viewModel.unableToFindPlaceCoordinatesError
                .compose(bindToLifecycle())
                .observeOn(mainThread())
                .subscribe({
                    Toast.makeText(
                            activity!!,
                            R.string.failed_to_resolve_location,
                            Toast.LENGTH_SHORT
                    ).show()
                }, errorLogger::trackError)
    }

    /**
     * @suppress
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LocationSearchBinding.inflate(inflater)

        binding.viewModel = viewModel
        searchView = binding.searchLayout.searchView

        binding.resultView.addItemDecoration(buildItemDecoration())

        initSearchView(binding.searchLayout.searchView)
        return binding.root
    }

    private fun buildItemDecoration(): DividerItemDecoration {
        val ATTRS = intArrayOf(android.R.attr.listDivider)
        val a = context!!.obtainStyledAttributes(ATTRS)
        val divider = a.getDrawable(0)
        val inset = resources.getDimensionPixelSize(R.dimen.tripkit_search_result_divider_inset)
        val insetDivider = InsetDrawable(divider, inset, 0, inset, 0)
        a.recycle()

        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(insetDivider)
        return itemDecoration
    }
    /**
     * @suppress
     */
    override fun onStart() {
        super.onStart()
    }

    /**
     * @suppress
     */
    override fun onStop() {
        super.onStop()
    }

    /**
     * @suppress
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.onCleared()
    }

    private fun initSearchView(searchView: SearchView) {
        val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

        // To show the keyboard initially.
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return viewModel.onTextSubmit()
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.onQueryTextChanged(newText)
                return true
            }
        })

        viewModel.handleArgs(arguments)
        viewModel.loadCity()
        arguments?.let {
            handleArguments(it, searchView)
        }
    }

    private fun handleArguments(arguments: Bundle, searchView: SearchView) {
        // Setting these queries will cause text changed,
        // so AutoComplete task will be triggered with MAX_VALUE nearLat, nearLon => crash.
        val initialQuery = arguments.getString(ARG_INITIAL_QUERY)
        searchView.setQuery(initialQuery, false)

        val queryHint = arguments.getString(ARG_QUERY_HINT)

        if (!TextUtils.isEmpty(queryHint)) {
            searchView.queryHint = queryHint
        }
    }

    fun dismissKeyboard() {
        searchView?.clearFocus()
    }

    /**
     * Used to create a new instance of the fragment.
     */
    class Builder {
        private var bounds: LatLngBounds? = null
        private var near: LatLng? = null
        private var initialQuery: String? = null
        private var hint: String? = null
        private var canOpenTimetable: Boolean = false
        private var withCurrentLocation: Boolean = false
        private var withDropPin: Boolean = false
        private var showBackButton: Boolean = true
        private var locationSearchIconProvider: LocationSearchIconProvider? = null
        /**
         * Used for Google Places searches. For example, a map's visible boundaries.
         *
         * @param bounds The boundaries for Google Places
         * @return this Builder
         */
        fun withBounds(bounds: LatLngBounds?): Builder {
            this.bounds = bounds
            return this
        }

        /**
         * Used for TripGo searches. For example, the center of a map.
         *
         * @param near Where to center TripGo searches on
         * @return this Builder
         */
        fun near(near: LatLng?): Builder {
            this.near = near
            return this
        }

        /**
         * Sets the initial query.
         *
         * @param initialQuery The query to pre-populate
         * @return this Builder
         */
        fun withInitialQuery(initialQuery: String?): Builder {
            this.initialQuery = initialQuery
            return this
        }

        /**
         * Sets the EditText hint. For example, "Where do you want to go?"
         *
         * @param hint A hint
         * @return this Builder
         */
        fun withHint(hint: String?): Builder {
            this.hint = hint
            return this
        }

        /**
         * The fragment can optionally show a static option of "Current Location".
         *
         * @param withCurrentLocation When **true**, show the "Current Location" option
         * @return this Builder
         */
        fun allowCurrentLocation(withCurrentLocation: Boolean = true): Builder {
            this.withCurrentLocation = withCurrentLocation
            return this
        }

        /**
         * The fragment can optionally show a static option of "Choose on Map".
         *
         * @param withDropPin When **true**, show the "Choose on Map" option
         * @return this Builder
         */
        fun allowDropPin(withDropPin: Boolean = true): Builder {
            this.withDropPin = withDropPin
            return this
        }


        /**
         * Should the search box include a back button?
         * @param showBackButton When **true**, show a back button which dismisses the fragment when clicked.
         * @return this Builder
         */
        fun showBackButton(showBackButton: Boolean = true): Builder {
            this.showBackButton = showBackButton
            return this
        }

        /**
         * If you want to use your own [LocationSearchIconProvider], provide it here.
         * @param locationSearchIconProvider An instance of a [LocationSearchIconProvider]
         * @return this Builder
         */
        fun withLocationSearchIconProvider(locationSearchIconProvider: LocationSearchIconProvider): Builder {
            this.locationSearchIconProvider = locationSearchIconProvider
            return this
        }
        /**
         * Finalize and build the Fragment
         *
         * @return A usable LocationSearchFragment
         */
        fun build(): LocationSearchFragment {
            val args = Bundle()
            args.putParcelable(KEY_BOUNDS, bounds)
            args.putParcelable(KEY_CENTER, near)
            args.putString(ARG_QUERY_HINT, hint)
            args.putString(ARG_INITIAL_QUERY, initialQuery)
            args.putBoolean(ARG_CAN_OPEN_TIMETABLE, canOpenTimetable)
            args.putBoolean(ARG_WITH_CURRENT_LOCATION, withCurrentLocation)
            args.putBoolean(ARG_WITH_DROP_PIN, withDropPin)
            args.putBoolean(ARG_SHOW_BACK_BUTTON, showBackButton)

            val fragment = LocationSearchFragment()
            fragment.setArguments(args)
            fragment.locationSearchIconProvider = locationSearchIconProvider
            return fragment
        }
    }


}
