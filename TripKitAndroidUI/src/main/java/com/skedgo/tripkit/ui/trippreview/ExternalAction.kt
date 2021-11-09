package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.webkit.URLUtil
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.checkUrl
import com.skedgo.tripkit.ui.utils.getPackageNameFromStoreUrl
import com.skedgo.tripkit.ui.utils.isAppInstalledById

data class Action(
        var data: String?, //url or package
        var appInstalled: Boolean,
        var drawable: Int,
        var fallbackUrl: String? = null
)

fun Context.handleExternalAction(dataUrl: String): Action? {

    var url = dataUrl

    if (!URLUtil.isNetworkUrl(dataUrl)) {
        url = getUrlByApp(dataUrl) ?: dataUrl
    }

    return url.getPackageNameFromStoreUrl()?.let { appId ->
        val appInstalled = appId.isAppInstalledById(packageManager)
        return Action(if (appInstalled) {
            appId
        } else {
            url.checkUrl()
        }, appInstalled, getExternalActionDrawable(appInstalled, isApp = true, isTel = false))
    } ?: Action(url.checkUrl(), false, getExternalActionDrawable(false, isApp = false, isTel = url.startsWith("tel:")))
}

fun getUrlByApp(app: String): String? {
    return when (app) {
        "gocatch" -> {
            "https://play.google.com/store/apps/details?id=com.gocatchapp.goCatch"
        }
        "ingogo" -> {
            "https://play.google.com/store/apps/details?id=mobi.ingogo.driver"
        }
        else -> {
            null
        }
    }
}

fun getExternalActionDrawable(appInstalled: Boolean, isApp: Boolean, isTel: Boolean): Int {

    return if (appInstalled || isApp) {
        R.drawable.ic_open
    } else if (isTel) {
        R.drawable.ic_call
    } else {
        R.drawable.ic_globe
    }
}