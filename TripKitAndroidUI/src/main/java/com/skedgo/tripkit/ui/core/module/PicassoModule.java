package com.skedgo.tripkit.ui.core.module;

import android.content.Context;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

import javax.inject.Singleton;
import java.io.File;

/**
 * Defines components fetching and loading images.
 */
@Module
public class PicassoModule {
  static Cache createCache(Context context) {
    final File imagesCacheDir = new File(context.getCacheDir(), "picasso-images");
    final int cacheSize = 10 * 1024 * 1024; // 10 MB.
    return new Cache(imagesCacheDir, cacheSize);
  }

  @Provides @Singleton Picasso picasso(
      Context context,
      OkHttpClient httpClient
  ) {
    // Use an own HttpClient in order not to
    // interfere w/ other sorts of requests.
    OkHttpClient.Builder builder = httpClient.newBuilder();
    builder.interceptors().clear();
    builder.networkInterceptors().clear();
    final OkHttpClient downloader = builder
        .cache(createCache(context))
        .build();
    return new Picasso.Builder(context)
        .downloader(new OkHttp3Downloader(downloader))
        .build();
  }
}