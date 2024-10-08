package com.skedgo.tripkit.ui.locationpointer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.AndroidGeocoder
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryRepository
import com.skedgo.tripkit.ui.utils.TapAction
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LocationPointerViewModel @Inject constructor(
    val context: Context,
    val locationHistoryRepository: LocationHistoryRepository
) : RxViewModel() {
    var mapIdleThrottle = PublishSubject.create<LatLng>()
    var geocoder = AndroidGeocoder(context)
    var geocodeResult = PublishSubject.create<LatLng>()

    private var _canChoose = MutableLiveData<Boolean>(false)
    val canChoose: LiveData<Boolean> = _canChoose

    private var _showSpinner = MutableLiveData<Boolean>(false)
    val showSpinner: LiveData<Boolean> = _showSpinner

    private var _showInfo = MutableLiveData<Boolean>(false)
    val showInfo: LiveData<Boolean> = _showInfo

    private var _locationText = MutableLiveData<String>("")
    val locationText: LiveData<String> = _locationText

    //var canChoose = ObservableBoolean(false)
    //var showSpinner = ObservableBoolean(false)
    //var showInfo = ObservableBoolean(false)
    //var locationText = ObservableField<String>("")
    val doneClicked: TapAction<LocationPointerViewModel> = TapAction.create { this }
    var currentLatLng = LatLng(0.0, 0.0)
    var currentAddress = String()

    init {
        mapIdleThrottle.debounce(500, TimeUnit.MILLISECONDS)
            .subscribe({ geocodeResult.onNext(it) }, { Timber.e(it) })
            .autoClear()

        geocodeResult.hide()
            .observeOn(Schedulers.io())
            .switchMap {
                currentLatLng = it
                geocoder.getAddress(it.latitude, it.longitude)
            }
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribe({
                currentAddress = it
                _locationText.postValue(it)
                _canChoose.postValue(true)
                _showSpinner.postValue(false)
                _showInfo.postValue(true)
            }, {
                Timber.e(it)
                currentLatLng = LatLng(0.0, 0.0)
                currentAddress = ""
                _locationText.postValue("Could not load location")
                _canChoose.postValue(false)
                _showSpinner.postValue(false)
                _showInfo.postValue(false)
            }).autoClear()
    }

    fun setLocationText(value: String) {
        _locationText.postValue(value)
    }

    fun mapMoveStarted() {
        _locationText.postValue("")
        _canChoose.postValue(false)
        _showSpinner.postValue(true)
    }

    fun saveLocation(location: Location) {
        locationHistoryRepository.saveLocationsToHistory(
            listOf(location)
        ).observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe({}, {
                it.printStackTrace()
            }).addTo(compositeSubscription)
    }
}
