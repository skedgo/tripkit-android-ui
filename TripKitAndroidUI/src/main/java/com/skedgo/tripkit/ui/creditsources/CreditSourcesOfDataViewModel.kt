package com.skedgo.tripkit.ui.creditsources

import android.content.Context
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tracking.Event
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.skedgo.tripkit.ui.utils.TapAction
import dagger.Lazy
import com.skedgo.tripkit.routing.Source
import javax.inject.Inject

open class CreditSourcesOfDataViewModel @Inject internal constructor(
    private val context: Context,
    private val eventTrackerLazy: Lazy<EventTracker>
) {
    private var sources: List<Source> = emptyList()
    val creditSources = ObservableField<String>()
    val tapAction = TapAction.create { sources }

    init {
        tapAction.observable
            .map { sources.toCredits() }
            .subscribe { eventTrackerLazy.get().log(Event.ViewCreditSources(it!!)) }
    }

    fun changeSources(sources: List<Source>) {
        this.sources = sources
        creditSources.set(
            context.resources.getString(
                R.string.data_provided_by__pattern,
                sources.toCredits()
            )
        )
    }

    private fun List<Source>.toCredits(): String? = map { it.provider()?.name() }
        .reduce { names, newName ->
            "$names, $newName"
        }
}