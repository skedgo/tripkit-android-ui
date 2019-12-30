package com.skedgo.tripkit.ui.core.module
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.TimeLabelMaker
import com.skedgo.tripkit.ui.map.adapter.SegmentInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ServiceStopInfoWindowAdapter
import com.skedgo.tripkit.ui.routing.SegmentCameraUpdateRepository
import com.skedgo.tripkit.ui.routingresults.FetchingRealtimeStatusRepository
import com.skedgo.tripkit.ui.routingresults.GetSelectedTrip
import com.skedgo.tripkit.ui.routingresults.SelectedTripGroupRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresult.IsLocationPermissionGranted
import dagger.Module
import dagger.Provides

@Module
class TripDetailsModule {

  @ActivityScope
  @Provides
  fun selectedTripGroupRepository(
      tripGroupRepository: TripGroupRepository
  ): SelectedTripGroupRepository = SelectedTripGroupRepository(tripGroupRepository)

  @ActivityScope
  @Provides
  fun fetchingRealtimeStatusRepository(): FetchingRealtimeStatusRepository =
      FetchingRealtimeStatusRepository()

  @Provides fun isLocationPermissionGranted(context: Context): IsLocationPermissionGranted =
      IsLocationPermissionGranted(context)

//
//  @Provides internal fun saveUrlFetcher(httpClient: okhttp3.OkHttpClient, gson: Gson): SaveUrlFetcher {
//    return Retrofit.Builder()
//            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .baseUrl(Server.ApiTripGo.value)
//            .client(httpClient)
//            .build()
//            .create(SaveUrlFetcher::class.java)
//  }


  @Provides fun timeLabelMaker(context: Context): TimeLabelMaker {
    // It's okay to pass rootView as null into inflate() in this case.
    val inflater: LayoutInflater = LayoutInflater.from(context)
    val timeTextView = inflater.inflate(R.layout.view_time_label, null) as TextView
    return TimeLabelMaker(timeTextView)
  }

  @Provides fun segmentInfoWindowAdapter(context: Context): SegmentInfoWindowAdapter {
    val inflater: LayoutInflater = LayoutInflater.from(context)
    return SegmentInfoWindowAdapter(inflater)
  }

  @Provides fun serviceStopInfoWindowAdapter(context: Context): ServiceStopInfoWindowAdapter {
    val inflater: LayoutInflater = LayoutInflater.from(context)
    return ServiceStopInfoWindowAdapter(inflater)
  }

//  @Provides internal fun isLocationPermissionGranted(): IsLocationPermissionGranted =
//          IsLocationPermissionGranted(activity)

  @Provides
  @ActivityScope
  fun segmentCameraUpdateRepository(getSelectedTrip: GetSelectedTrip): SegmentCameraUpdateRepository
          = SegmentCameraUpdateRepository(getSelectedTrip = getSelectedTrip)
}
