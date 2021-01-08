package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsDataBase
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsRepository
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsRepositoryImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class FavoriteTripsModule {
    @Provides
    @Singleton
    internal fun favoriteTripsDatabase(context: Context): FavoriteTripsDataBase {
        return FavoriteTripsDataBase.getInstance(context)
    }
    @Provides
    fun favoriteTripsRepository(db: FavoriteTripsDataBase): FavoriteTripsRepository =
        FavoriteTripsRepositoryImpl(db)
}