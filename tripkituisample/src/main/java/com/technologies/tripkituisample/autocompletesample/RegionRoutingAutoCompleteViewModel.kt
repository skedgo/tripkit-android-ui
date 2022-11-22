package com.technologies.tripkituisample.autocompletesample

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.regionrouting.data.RegionRoute
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.utils.configureInterceptor
import io.reactivex.subjects.PublishSubject

class RegionRoutingAutoCompleteViewModel : RxViewModel() {

    private val routeAutoCompletePublishSubject = PublishSubject.create<String>()

    private val _regionRoutes = MutableLiveData<List<RegionRoute>>()
    val regionRoutes: LiveData<List<RegionRoute>> = _regionRoutes

    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> = _error

    private var location: Location? = Location(-33.9504502,151.0309)

    init {
        routeAutoCompletePublishSubject.configureInterceptor(500)
                .subscribe {
                    handleSearch(it)
                }.autoClear()
    }

    fun onSearchQueryChange(text: CharSequence) {
        routeAutoCompletePublishSubject.onNext(text.toString())
    }

    private fun handleSearch(query: String) {

        TripKitUI.getInstance().regionRoutingRepository().getRoutes(query, location)
                .subscribe({
                    _regionRoutes.postValue(it)
                }, {
                    _error.postValue(it)
                }).autoClear()
    }

}