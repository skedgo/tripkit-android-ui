package com.skedgo.tripkit.ui.core

import android.graphics.Bitmap
import androidx.annotation.DimenRes
import com.squareup.picasso.Picasso
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers


fun Picasso.fetchAsync(url: String): Single<Bitmap> =
        fetchAsyncWithSize(url, null).observeOn(Schedulers.io()).subscribeOn(mainThread())

fun Picasso.fetchAsyncWithSize(url: String, @DimenRes maxSize: Int? = null): Single<Bitmap> {
    return Single.create {
        try {
            if (!it.isDisposed) {
                val creator = this.load(url)
                maxSize?.let {
                    creator.resizeDimen(it,it)
                            .centerInside()
                            .onlyScaleDown()
                }
                val bitmap = creator.get()
                it.onSuccess(bitmap)
            }
        } catch (e: Throwable) {
            it.onError(UnableToFetchBitmapError("Unable to fetch bitmap $url: ${e.message}"))
        }
    }
}



class UnableToFetchBitmapError(message: String) : RuntimeException(message)