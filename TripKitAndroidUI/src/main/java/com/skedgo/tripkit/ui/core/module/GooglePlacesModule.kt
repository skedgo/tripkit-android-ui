package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepositoryImpl
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class GooglePlacesModule {
    @Provides
    internal fun placeSearchRepository(context: Context): PlaceSearchRepository =
        PlaceSearchRepositoryImpl(Provider {
            com.google.android.libraries.places.api.Places.createClient(
                context
            )
        })
}
