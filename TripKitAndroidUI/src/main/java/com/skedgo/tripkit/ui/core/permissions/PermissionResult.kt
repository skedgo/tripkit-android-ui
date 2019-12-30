package com.skedgo.tripkit.ui.core.permissions

sealed class PermissionResult {
  data class Granted(val permissions: Array<out String>) : PermissionResult()
  data class Denied(val permissions: Array<out String>) : PermissionResult()
  data class DeniedWithNeverAskAgain(val permissions: Array<out String>) : PermissionResult()
}
