package com.skedgo.tripkit.ui.core.permissions

import io.reactivex.Observable
import io.reactivex.Single

interface CanRequestPermission {
  fun checkSelfPermissionReactively(permission: String): Observable<Boolean>
  fun requestPermissions(
      permissionsRequest: PermissionsRequest,
      onRationale: () -> Single<ActionResult>,
      onNeverAskAgainDenial: () -> Unit
  ): Single<PermissionResult>
}