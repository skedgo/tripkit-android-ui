package com.skedgo.tripkit.ui.data.personaldata

import android.content.SharedPreferences
import com.skedgo.tripkit.data.util.onChanged
import com.skedgo.tripkit.ui.personaldata.MyPersonalDataRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

import javax.inject.Inject
import javax.inject.Named

class MyPersonalDataRepositoryImpl @Inject constructor(
        @Named("MyPersonalData") private val sharedPreferences: SharedPreferences) : MyPersonalDataRepository {

    companion object {
        const val tripProgress = "tripProgress"
        const val tripSelection = "tripSelection"
        const val appUsage = "appUsage"
    }

    override fun isUploadTripProgressEnabled(): Single<Boolean> {
        return Single.fromCallable {
            sharedPreferences.getBoolean(tripProgress, false)
        }
    }

    override fun isUploadTripSelectionEnabled(): Single<Boolean> {
        return Single.fromCallable {
//      sharedPreferences.getBoolean(tripSelection, true) // TripSelection is opt-out
            true
        }
    }

    override fun isUploadAppUsageEnabled(): Single<Boolean> {
        return Single.fromCallable {
            sharedPreferences.getBoolean(appUsage, false)
        }
    }

    override fun setUploadTripProgressEnabled(enabled: Boolean): Completable {
        return Completable
                .fromAction {
                    sharedPreferences.edit()
                            .putBoolean(tripProgress, enabled)
                            .apply()
                }
    }

    override fun setUploadTripSelectionEnabled(enabled: Boolean): Completable {
        return Completable
                .fromAction {
                    sharedPreferences.edit()
                            .putBoolean(tripSelection, enabled)
                            .apply()
                }
    }

    override fun setUploadAppUsageEnabled(enabled: Boolean): Completable {
        return Completable
                .fromAction {
                    sharedPreferences.edit()
                            .putBoolean(appUsage, enabled)
                            .apply()
                }
    }

    override fun onChanges(): Observable<Unit> {
        return sharedPreferences.onChanged()
                .map { Unit }
    }
}