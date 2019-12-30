package com.skedgo.tripkit.ui.tripresult
import android.content.Context
import io.reactivex.Observable

open class IsLocationPermissionGranted constructor(val context: Context) {
  /**
   * Checks if given permission is granted or not. If it's granted, the returned signal
   * will emit true & complete. Otherwise, waits until the permission is granted later.
   * Once it's granted, it will emit true but never complete.
   */
//  open operator fun invoke(): Observable<Boolean> =  Observable
//          .fromCallable {
//            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
//          }
//          .flatMap {
//            when (it) {
//              PackageManager.PERMISSION_GRANTED -> Observable.just(true)
//              else -> requestPermissionsResult.asObservable()
//                      .filter { it.permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) }
//                      .map { it.grantResults[it.permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)] }
//                      .map { it == PackageManager.PERMISSION_GRANTED }
//            }
//          }
  // TODO: Need to check if the permission is really granted
    open operator fun invoke(): Observable<Boolean> = Observable.just(true)
}
