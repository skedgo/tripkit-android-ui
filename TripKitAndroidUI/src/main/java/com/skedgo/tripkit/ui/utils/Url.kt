package com.skedgo.tripkit.ui.utils

import android.content.pm.PackageManager
import android.net.Uri

fun String.isAppInstalled(packageManager: PackageManager): Boolean{

    val uri = Uri.parse(this)

    return uri.getQueryParameter("id")?.let {
        try {
            packageManager.getPackageInfo(it,0)
            true
        }catch (e: PackageManager.NameNotFoundException){
            false
        }
    }?: false
}

fun String.getPackageNameFromStoreUrl(packageManager: PackageManager): String?{
    return Uri.parse(this).getQueryParameter("id")
}