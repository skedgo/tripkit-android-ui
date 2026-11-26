package com.technologies.tripkituisample

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Sample-only host appearance setting; the SDK never changes the application's night mode. */
internal object SampleAppearance {
    fun mode(context: Context): Int = context.getSharedPreferences("sample_appearance", Context.MODE_PRIVATE)
        .getInt("mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun apply(context: Context) = AppCompatDelegate.setDefaultNightMode(mode(context))

    fun select(context: Context, mode: Int) {
        context.getSharedPreferences("sample_appearance", Context.MODE_PRIVATE)
            .edit().putInt("mode", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
