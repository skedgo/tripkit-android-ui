package com.skedgo.tripkit.ui.core.permissions

import io.reactivex.Single

interface CanRequestPermission {
  fun requestPermissions(
      permissionsRequest: PermissionsRequest,
      onRationale: () -> Single<ActionResult>,
      onNeverAskAgainDenial: () -> Unit
  ): Single<PermissionResult>
}