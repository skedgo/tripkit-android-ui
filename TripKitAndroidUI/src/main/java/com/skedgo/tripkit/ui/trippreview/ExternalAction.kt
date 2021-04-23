package com.skedgo.tripkit.ui.trippreview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.skedgo.tripkit.ui.utils.checkUrl
import com.skedgo.tripkit.ui.utils.getPackageNameFromStoreUrl
import com.skedgo.tripkit.ui.utils.isAppInstalledById

data class Action(
        val data: String, //url or package
        val appInstalled: Boolean
)

fun Context.handleExternalAction(dataUrl: String): Action? {

    dataUrl.getPackageNameFromStoreUrl()?.let {
        return Action(it, it.isAppInstalledById(packageManager))
    } ?: dataUrl.checkUrl()?.let {
        return Action(it, false)
    }

    return null
}