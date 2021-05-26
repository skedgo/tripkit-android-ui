package com.skedgo.tripkit.ui.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryRepository
import com.skedgo.tripkit.ui.databinding.LocationSearchBinding
import com.skedgo.tripkit.ui.utils.defocusAndHideKeyboard
import com.skedgo.tripkit.ui.utils.showKeyboard
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
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
class LocationSearchFragment : BaseTripKitFragment() {

    @Inject
    lateinit var locationHistoryRepository: LocationHistoryRepository

    lateinit var binding: LocationSearchBinding

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

    fun setOnLocationSelectedListener(listener: (Location) -> Unit) {
        this.locationSelectedListener = object : OnLocationSelectedListener {
            override fun onLocationSelected(location: Location) {
                saveLocationToHistory(location)
                listener(location)
            }

        }
    }

    /**
     * This callback will be invoked when the user chooses a city"
     */
    interface OnCitySuggestionSelectedListener {
        fun onCitySuggestionSelected(id: Location)
    }

    private var citySuggestionSelectedListener: OnCitySuggestionSelectedListener? = null
    fun setOnCitySuggestionSelectedListener(callback: OnCitySuggestionSelectedListener) {
        this.citySuggestionSelectedListener = callback
    }

    fun setOnCitySelectedListener(listener: (Location) -> Unit) {
        this.citySuggestionSelectedListener = object : OnCitySuggestionSelectedListener {
            override fun onCitySuggestionSelected(id: Location) {
                listener(id)
            }

        }
    }

    private fun saveLocationToHistory(location: Location) {
        if (location.name != getString(R.string.home) && location.name != getString(R.string.work)) {
            locationHistoryRepository.saveLocationsToHistory(
                    listOf(location)
            ).observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribe({}, {
                        it.printStackTrace()
                    }).addTo(autoDisposable)
        }
    }

    /**
     * This callback will be invoked when the user chooses "Current Location"
     */
    interface OnFixedSuggestionSelectedListener {
        fun onFixedSuggestionSelected(id: Any)
    }

    private var fixedSuggestionSelectedListener: OnFixedSuggestionSelectedListener? = null
    fun setOnFixedSuggestionSelectedListener(callback: OnFixedSuggestionSelectedListener) {
        this.fixedSuggestionSelectedListener = callback
    }

    fun setOnFixedSuggestionSelectedListener(listener: (Any) -> Unit) {
        this.fixedSuggestionSelectedListener = object : OnFixedSuggestionSelectedListener {
            override fun onFixedSuggestionSelected(id: Any) {
                listener(id)
            }
        }
    }

    /**
     * This callback will be invoked when a search result is clicked.
     */
    interface OnAttachFragmentListener {
        fun onAttachFragment()
    }

    private var onAttachFragmentListener: OnAttachFragmentListener? = null
    fun setOnAttachFragmentListener(callback: OnAttachFragmentListener) {
        this.onAttachFragmentListener = callback
    }

    /**
     * @suppress
     */
    @Inject
    lateinit var viewModelFactory: LocationSearchViewModelFactory
    private lateinit var viewModel: LocationSearchViewModel

    /**
     * @suppress
     */
    @Inject
    lateinit var errorLogger: ErrorLogger
    private var searchView: SearchView? = null
    private var showSearchFieldBoolean = true

    var locationSearchIconProvider: LocationSearchIconProvider? = null
        set(value) {
            field = value
            if (::viewModel.isInitialized) {
                viewModel.locationSearchIconProvider = value
            }
        }

    var fixedSuggestionsProvider: FixedSuggestionsProvider? = null
        set(value) {
            field = value
            if (::viewModel.isInitialized) {
                viewModel.fixedSuggestionsProvider = value
            }
        }

    var searchSuggestionProvider: LocationSearchProvider? = null
        set(value) {
            field = value
            if (::viewModel.isInitialized) {
                viewModel.locationSearchProvider = value
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
                .get(LocationSearchViewModel::class.java)
        viewModel.locationSearchIconProvider = locationSearchIconProvider
        viewModel.fixedSuggestionsProvider = fixedSuggestionsProvider
        viewModel.locationSearchProvider = searchSuggestionProvider
    }

    /**
     * @suppress
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LocationSearchBinding.inflate(inflater)

        binding.viewModel = viewModel
        searchView = binding.searchLayout.searchView

        binding.resultView.addItemDecoration(buildItemDecoration())

        initSearchView(binding.searchLayout.searchView)

        return binding.root
    }

    private fun buildItemDecoration(): DividerItemDecoration {
        val ATTRS = intArrayOf(android.R.attr.listDivider)
        val a = requireContext().obtainStyledAttributes(ATTRS)
        val divider = a.getDrawable(0)
        val inset = resources.getDimensionPixelSize(R.dimen.tripkit_search_result_divider_inset)
        val insetDivider = InsetDrawable(divider, inset, 0, 0, 0)
        a.recycle()

        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(insetDivider)
        return itemDecoration
    }

    override fun onResume() {
        super.onResume()
        viewModel.locationChosen
                .observeOn(mainThread())
                .subscribe({
                    locationSelectedListener?.onLocationSelected(it)
                }, errorLogger::trackError).addTo(autoDisposable)
        viewModel.fixedLocationChosen
                .observeOn(mainThread())
                .subscribe({
                    fixedSuggestionSelectedListener?.onFixedSuggestionSelected(it)
                }, errorLogger::trackError).addTo(autoDisposable)
        viewModel.cityLocationChosen
                .observeOn(mainThread())
                .subscribe({
                    citySuggestionSelectedListener?.onCitySuggestionSelected(it)
                }, errorLogger::trackError).addTo(autoDisposable)
        viewModel.dismiss
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
                }, errorLogger::trackError).addTo(autoDisposable)
        viewModel.unableToFindPlaceCoordinatesError
                .observeOn(mainThread())
                .subscribe({
                    Toast.makeText(
                            activity!!,
                            R.string.failed_to_resolve_location,
                            Toast.LENGTH_SHORT
                    ).show()
                }, errorLogger::trackError).addTo(autoDisposable)

        viewModel.scrollListToTop.asObservable()
                .observeOn(mainThread())
                .subscribe({
                    if (it) {
                        scrollResultsToTop()
                    }
                }, {
                    it.printStackTrace()
                }).addTo(autoDisposable)

        viewModel.onFinishLoad
                .observeOn(mainThread())
                .subscribe({
                    if (it) {
                        searchView?.post {
                            onAttachFragmentListener?.onAttachFragment()
                        }
                    }
                }, errorLogger::trackError).addTo(autoDisposable)

        searchView?.requestFocus()
        showKeyboard(requireActivity())
    }

    /**
     * @suppress
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.onCleared()
    }

    /**
     * Sets the search query manually. It's primarily useful if you've turned off the search box.
     *
     * @param query
     */
    fun setQuery(query: String, isRouting: Boolean = false) {
        viewModel.onQueryTextChanged(query, isRouting)
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

    private fun scrollResultsToTop() {
        if (this::binding.isInitialized) {
            binding.resultView.scrollToPosition(0)
        }
        viewModel.scrollListToTop.set(false)
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
        private var fixedSuggestionsProvider: FixedSuggestionsProvider? = null
        private var showSearchField: Boolean = true
        private var searchProvider: LocationSearchProvider? = null

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
         * The fragment can optionally show a static option of "Current Location". This will be ignored if you specify
         * your own [FixedSuggestionsProvider].
         *
         * @param withCurrentLocation When **true**, show the "Current Location" option
         * @return this Builder
         */
        fun allowCurrentLocation(withCurrentLocation: Boolean = true): Builder {
            this.withCurrentLocation = withCurrentLocation
            return this
        }

        /**
         * The fragment can optionally show a static option of "Choose on Map". This will be ignored if you specify
         * your own [FixedSuggestionsProvider].
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
         * Fixed search results (such as Choose on Map and Current Location) are shown when the search results are empty.
         * If you'd like to provide your own list of fixed results, you can speficy a [FixedSuggestionsProvider] which
         * will be queried. This is mutually exclusive with [allowCurrentLocation] and [allowDropPin].
         *
         * @param fixedSuggestionsProvider An instance of a [FixedSuggestionsProvider]
         * @return this Builder
         */
        fun withFixedSuggestionsProvider(fixedSuggestionsProvider: FixedSuggestionsProvider): Builder {
            this.fixedSuggestionsProvider = fixedSuggestionsProvider
            return this
        }

        /**
         * Additional search results can be provided (from a database, for example) using one or more LocationSearchProviders.
         */
        fun withLocationSearchProvider(locationSearchProvider: LocationSearchProvider): Builder {
            this.searchProvider = locationSearchProvider
            return this
        }

        /**
         * You can choose to not show the location search field, and instead orchestrate the search results on your own
         * by calling [setQuery].
         *
         * @param showSearchField When **true**, show the search field.
         * @return this Builder
         */
        fun showSearchField(showSearchField: Boolean): Builder {
            this.showSearchField = showSearchField
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
            args.putBoolean(ARG_SHOW_SEARCH_FIELD, showSearchField)
            val fragment = LocationSearchFragment()
            fragment.arguments = args
            fragment.searchSuggestionProvider = searchProvider
            fragment.locationSearchIconProvider = locationSearchIconProvider
            fragment.fixedSuggestionsProvider = fixedSuggestionsProvider
            return fragment
        }
    }


}
