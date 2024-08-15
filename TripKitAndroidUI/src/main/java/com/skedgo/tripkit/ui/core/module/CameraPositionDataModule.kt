package com.skedgo.tripkit.ui.core.module
import android.content.Context
import android.content.SharedPreferences
import com.skedgo.tripkit.camera.CachingDateTimeOfMapCameraPositionRepository
import com.skedgo.tripkit.camera.LastCameraPositionRepository
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.data.cameraposition.CachingDateTimeOfMapCameraPositionRepositoryImpl
import com.skedgo.tripkit.ui.data.cameraposition.LastCameraPositionRepositoryImpl
import dagger.Module
import dagger.Provides
import java.util.*

@Module
class CameraPositionDataModule {
  @Provides
  fun lastCameraPositionRepository(context: Context, regionService: RegionService): LastCameraPositionRepository =
      LastCameraPositionRepositoryImpl(context.resources, getMapPrefs(context), Locale.getDefault(), regionService)
  @Provides
  fun cachingDateTimeOfMapCameraPositionRepository(context: Context): CachingDateTimeOfMapCameraPositionRepository =
          CachingDateTimeOfMapCameraPositionRepositoryImpl(getMapPrefs(context))

  private fun getMapPrefs(context: Context): SharedPreferences
      = context.getSharedPreferences("MapPreferences", Context.MODE_PRIVATE)
}
