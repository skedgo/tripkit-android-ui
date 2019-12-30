package com.skedgo.tripkit.ui.core.module
import android.content.Context
import com.skedgo.tripkit.ui.core.modeprefs.IsModeMinimizedRepositoryImpl
import com.skedgo.tripkit.ui.routingresults.IsModeMinimizedRepository
import dagger.Module
import dagger.Provides

@Module class IsModeMinimizedRepositoryModule {
  @Provides fun isModeMinimizedRepository(context: Context): IsModeMinimizedRepository =
      IsModeMinimizedRepositoryImpl(context.getSharedPreferences(
          "IsModeMinimizedPrefs",
          Context.MODE_PRIVATE
      ))
}