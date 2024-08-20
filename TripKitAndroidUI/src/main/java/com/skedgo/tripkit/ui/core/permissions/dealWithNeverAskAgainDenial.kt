package com.skedgo.tripkit.ui.core.permissions

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.viewAppDetailsSettingsIntent

fun Activity.dealWithNeverAskAgainDenial(message: String): () -> Unit = {
    Snackbar.make(findContentLayout(), message, Snackbar.LENGTH_LONG)
        .setAction(R.string.settings, { startActivity(viewAppDetailsSettingsIntent()) })
        .show()
}

fun Activity.findContentLayout(): View = findViewById(android.R.id.content)