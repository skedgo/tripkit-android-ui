package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.google.gson.Gson
import com.skedgo.tripkit.data.database.timetables.ServiceAlertDataModule
import com.skedgo.tripkit.ui.data.CursorToServiceConverter
import com.skedgo.tripkit.ui.timetables.domain.TimetableLoaderFactory
import dagger.Module
import dagger.Provides

@Module(includes = [ServiceAlertDataModule::class])
class TimetableModule(private val activityContext: Context) {

  @Provides
  internal fun provideTimetableLoaderFactory(): TimetableLoaderFactory {
    return TimetableLoaderFactory()
  }
//
//  @Provides
//  internal fun provideTimetableAdapter(cursorToServiceConverter: CursorToServiceConverter,
//                                       updateServiceAlerts: UpdateServiceAlerts,
//                                       getClosestPositionToNow: GetClosestPositionToNow): TimetableAdapter {
//    return TimetableAdapter(activityContext, cursorToServiceConverter, updateServiceAlerts, getClosestPositionToNow)
//  }

  @Provides
  internal fun provideCursorToServiceConverter(gson: Gson): CursorToServiceConverter {
    return CursorToServiceConverter(gson)
  }

}