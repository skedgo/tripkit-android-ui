package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoritesDatabase
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesApi
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository.FavoritesRepositoryImpl
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
class FavoritesModule {
    @Provides
    @Singleton
    internal fun favoritesDatabase(context: Context): FavoritesDatabase {
        return FavoritesDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    internal fun favoritesApi(builder: Retrofit.Builder, httpClient: OkHttpClient): FavoritesApi {
        return builder
            .client(httpClient)
            .build().create(FavoritesApi::class.java)
    }

    @Provides
    fun favoritesRepository(
        favoritesApi: FavoritesApi,
        db: FavoritesDatabase
    ): FavoritesRepository =
        FavoritesRepositoryImpl(favoritesApi, db.favoriteDao())


}