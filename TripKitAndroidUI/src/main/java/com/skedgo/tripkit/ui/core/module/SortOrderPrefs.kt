package com.skedgo.tripkit.ui.core.module

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Named

const val SortOrderPrefs = "SortOrderPrefs"

@Module
class RoutesModule(private val activityContext: Context) {

    @Provides
    @Named(SortOrderPrefs)
    fun sortOrderPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(
            "SortOrder",
            Context.MODE_PRIVATE
        )
}
