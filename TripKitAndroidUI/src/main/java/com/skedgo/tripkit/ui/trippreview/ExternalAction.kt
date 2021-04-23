package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.checkUrl
import com.skedgo.tripkit.ui.utils.getPackageNameFromStoreUrl
import com.skedgo.tripkit.ui.utils.isAppInstalledById

data class Action(
        val data: String?, //url or package
        val appInstalled: Boolean,
        var drawable: Int,
        var fallbackUrl: String? = null
)

fun Context.handleExternalAction(dataUrl: String): Action? {

    return dataUrl.getPackageNameFromStoreUrl()?.let { appId ->
        val appInstalled = appId.isAppInstalledById(packageManager)
        return Action(if (appInstalled) {
            appId
        } else {
            dataUrl.checkUrl()
        }, appInstalled, getExternalActionDrawable(appInstalled))
    } ?: Action(dataUrl.checkUrl(), false, getExternalActionDrawable(false))
}

fun getExternalActionDrawable(appInstalled: Boolean): Int {
    return if (appInstalled) {
        R.drawable.ic_open
    } else {
        R.drawable.ic_globe
    }
}