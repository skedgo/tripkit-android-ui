package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import com.skedgo.rxtry.toTry
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.location.UserGeoPointRepository
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
class TKUIHomeViewControllerViewModel @Inject constructor(
    private val context: Context,
    userGeoPointRepository: UserGeoPointRepository
) : RxViewModel() {

    private val _state = MutableLiveData(TKUIHomeViewControllerUIState())
    val state: LiveData<TKUIHomeViewControllerUIState> = _state

    private val _myLocationButtonVisible = MutableLiveData(false)
    val myLocationButtonVisible: LiveData<Boolean> = _myLocationButtonVisible

    private val userGeoPointObservable =
        userGeoPointRepository.getFirstCurrentGeoPoint()
            .toTry()
            .map<Try<Location>> { tried: Try<GeoPoint> ->
                when (tried) {
                    is Success -> {
                        val location =
                            Location(tried.invoke().latitude, tried.invoke().longitude).also {
                                it.name = context.resources.getString(R.string.current_location)
                            }
                        Success(location)
                    }

                    is Failure -> Failure<Location>(tried())
                }
            }.subscribeOn(Schedulers.io())

    fun getUserGeoPoint(location: (Try<Location>) -> Unit) {
        userGeoPointObservable.subscribe(
            {
                location.invoke(it)
            }, {
                Timber.e(it)
            }
        ).autoClear()
    }

    fun setMyLocationButtonVisible(isVisible: Boolean) {
        _myLocationButtonVisible.postValue(isVisible)
    }

    fun toggleChooseOnMap(isShow: Boolean) {
        _state.postValue(
            _state.value?.copy(isChooseOnMap = isShow)
        )
    }
}