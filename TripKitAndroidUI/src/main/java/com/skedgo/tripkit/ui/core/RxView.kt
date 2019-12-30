package com.skedgo.tripkit.ui.core
import android.view.View
import com.jakewharton.rxbinding3.view.globalLayouts
import io.reactivex.Single

fun View.afterMeasured(): Single<Unit> =
    globalLayouts().filter {this.width != 0 && this.height != 0 }.first(Unit)
