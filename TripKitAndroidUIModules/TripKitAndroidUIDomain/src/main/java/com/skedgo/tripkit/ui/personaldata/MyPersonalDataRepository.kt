package com.skedgo.tripkit.ui.personaldata

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface MyPersonalDataRepository {

  fun isUploadTripProgressEnabled(): Single<Boolean>
  fun isUploadTripSelectionEnabled(): Single<Boolean>
  fun isUploadAppUsageEnabled(): Single<Boolean>

  fun setUploadTripProgressEnabled(enabled: Boolean): Completable
  fun setUploadTripSelectionEnabled(enabled: Boolean): Completable
  fun setUploadAppUsageEnabled(enabled: Boolean): Completable

  fun onChanges(): Observable<Unit>
}