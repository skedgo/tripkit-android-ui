package com.skedgo.tripkit.ui.core

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.configuration.Key

/*
    This ContentProvider uses the trick that Firebase uses to initialize TripKit and TripKitUI's dependency injection.
    It should automatically be initialized before all other content providers, as well as any app.

    https://firebase.googleblog.com/2016/12/how-does-firebase-initialize-on-android.html
 */
class InitializerContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let {
            TripKitUI.initialize(context, Key.ApiKey(it.getString(R.string.skedgo_api_key)));
        }
        return true
    }

    // Verify that the user set their applicationId correctly, otherwise the authority's placeholder won't work
    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        if (info == null) {
            throw NullPointerException("TripKitUI's ProviderInfo cannot be null.");
        }

        // Double check that the API key is valid
        context?.let {
            if ("SKEDGO_API_KEY" == it.getString(R.string.skedgo_api_key)) {
                throw java.lang.IllegalStateException("In order to use TripKit UI, you need to add a string resource "
                + "called skedgo_api_key, and set it to your SkedGo API key.")
            }
        }

        // If the authorities equal the library internal ones, the developer didn't set his applicationId
        if ("com.skedgo.tripkit.ui.tripkit.initializer".equals(info.authority)) {
            throw IllegalStateException("Incorrect provider authority in manifest. Most likely due to a "
                    + "missing applicationId variable in application\'s build.gradle.");
        }

        super.attachInfo(context, info)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int = 0
}
