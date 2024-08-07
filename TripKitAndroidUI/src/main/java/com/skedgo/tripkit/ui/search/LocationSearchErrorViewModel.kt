package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.ui.R
import io.reactivex.Observable
import javax.inject.Inject

class LocationSearchErrorViewModel @Inject constructor(
    val context: Context
) {
    private val retryPublisher: PublishRelay<Unit> = PublishRelay.create<Unit>()
    private val chooseOnMapPublisher: PublishRelay<Unit> = PublishRelay.create<Unit>()

    open val title: ObservableField<String> = ObservableField()
    open val actionText: ObservableField<String> = ObservableField()
    open val iconSrc: ObservableField<Drawable> = ObservableField()

    private var errorType: SearchErrorType? = null

    open val retryObservable: Observable<Unit>
        get() = retryPublisher.hide()

    open val chooseOnMapObservable: Observable<Unit>
        get() = chooseOnMapPublisher.hide()

    open fun performAction() {
        when (errorType) {
            is SearchErrorType.NoResults -> chooseOnMapPublisher.accept(Unit)
            else -> retryPublisher.accept(Unit)
        }
    }

    open fun updateError(errorType: SearchErrorType?) {
        this.errorType = errorType
        when (errorType) {
            is SearchErrorType.NoResults -> {
                title.set(
                    context.getString(
                        R.string._apost_pattern_apost_not_found_dot,
                        errorType.searchText
                    )
                )
                actionText.set(context.getString(R.string.drop_new_pin))
                iconSrc.set(ContextCompat.getDrawable(context, R.drawable.ic_empty_result))
            }
            is SearchErrorType.NoConnection -> {
                title.set(context.getString(R.string.an_unexpected_network_error_has_occurred_dot_please_retry_dot))
                actionText.set(context.getString(R.string.retry))
                iconSrc.set(ContextCompat.getDrawable(context, R.drawable.ic_no_connection))
            }
            is SearchErrorType.OtherError -> {
                title.set(context.getString(R.string.error_encountered))
                actionText.set(context.getString(R.string.retry))
                iconSrc.set(ContextCompat.getDrawable(context, R.drawable.ic_empty_result))
            }
            else -> {
                // Do nothing
            }
        }
    }
}
