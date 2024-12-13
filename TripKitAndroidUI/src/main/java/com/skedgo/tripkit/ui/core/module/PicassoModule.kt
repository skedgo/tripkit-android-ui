package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.Builder
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

/**
 * Defines components fetching and loading images.
 */
@Module
class PicassoModule {
    @Provides
    @Singleton
    fun picasso(
        context: Context,
        httpClient: OkHttpClient
    ): Picasso {
        // Use an own HttpClient in order not to
        // interfere w/ other sorts of requests.
        val builder: OkHttpClient.Builder = httpClient.newBuilder()
        builder.interceptors().clear()
        builder.networkInterceptors().clear()
        val downloader: OkHttpClient = builder
            .cache(createCache(context))
            .build()
        return Builder(context)
            .downloader(OkHttp3Downloader(downloader))
            .build()
    }

    companion object {
        fun createCache(context: Context): Cache {
            val imagesCacheDir = File(context.cacheDir, "picasso-images")
            val cacheSize = 10 * 1024 * 1024 // 10 MB.
            return Cache(imagesCacheDir, cacheSize.toLong())
        }
    }
}