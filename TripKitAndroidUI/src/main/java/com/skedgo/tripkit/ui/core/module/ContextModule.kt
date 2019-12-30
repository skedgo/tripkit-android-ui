package com.skedgo.tripkit.ui.core.module;

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ContextModule(private val appContext: Context) {
    @Provides
    fun context(): Context {
        return appContext
    }
}