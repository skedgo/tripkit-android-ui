package com.skedgo.tripkit.ui.core

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DimenRes
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers

fun Picasso.fetchAsync(url: String): Single<Bitmap> =
    fetchAsyncWithSize(url, null)

fun Picasso.fetchAsyncWithSize(url: String, @DimenRes maxSize: Int? = null): Single<Bitmap> {
  return Single
      .create<Bitmap> { emitter ->
        val target: Target = object : Target {
          override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

          override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) =
              emitter.onError(UnableToFetchBitmapError("Unable to fetch bitmap $url"))

          override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) =
              emitter.onSuccess(bitmap)
        }
        val creator = this.load(url)
        maxSize?.let {
          creator.resizeDimen(it, it)
              .centerInside()
              .onlyScaleDown()
        }
        creator.into(target)
        emitter.setCancellable { this.cancelRequest(target) }
      }
      .subscribeOn(mainThread())
      .observeOn(Schedulers.io())
}

class UnableToFetchBitmapError(message: String) : RuntimeException(message)