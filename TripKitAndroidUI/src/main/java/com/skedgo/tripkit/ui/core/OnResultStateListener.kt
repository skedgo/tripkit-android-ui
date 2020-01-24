package com.skedgo.tripkit.ui.core

import android.view.View


/**
 * This listener is used when a certain SDK view (such as trip results) needs to display an empty view or an error view.
 */
interface OnResultStateListener {
    fun provideErrorView(message: String): View
    fun provideEmptyView(): View
}