package com.technologies.tripkituisample.autocompleter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.regionrouting.RegionRoutingAutoCompleter
import com.skedgo.tripkit.regionrouting.data.RegionRoute
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class AutocompleterViewModel @Inject constructor() : RxViewModel() {

    private val routeAutoCompletePublishSubject = PublishSubject.create<String>()

    private val _regionRoutes = MutableLiveData<List<RegionRoute>>()
    val regionRoutes: LiveData<List<RegionRoute>> = _regionRoutes

    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> = _error

    private var location: Location? = Location(-33.9504502, 151.0309)

    private val regionRoutingAutoCompleter: RegionRoutingAutoCompleter by lazy {
        TripKitUI.getInstance().regionRoutingAutoCompleter()
    }

    init {
        regionRoutingAutoCompleter.observe({
            _regionRoutes.postValue(it)
        }, {
            it.printStackTrace()
        }).autoClear()
    }

    fun onSearchQueryChange(text: CharSequence) {
        regionRoutingAutoCompleter.sendQuery(
            RegionRoutingAutoCompleter.AutoCompleteQuery.Builder(
                text.toString()
            ).byRegionName("US_CA_LosAngeles").build()
        )
    }
}