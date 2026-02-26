package com.skedgo.tripkit.ui.search.compose

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.search.FixedSuggestionViewModel
import com.skedgo.tripkit.ui.search.FixedSuggestions
import com.skedgo.tripkit.ui.search.GoogleAndTripGoSuggestionViewModel
import com.skedgo.tripkit.ui.search.HistorySearchProviderSuggestionViewModel
import com.skedgo.tripkit.ui.search.SuggestionViewModel

@Immutable
data class SearchResultRowUiModel(
    val title: String,
    val subtitle: String?,
    val matcher: String?,
    val titleTextColor: Int,
    val subtitleTextColor: Int,
    val icon: Drawable?,
    val shouldTintIcon: Boolean,
    val showTimetableIcon: Boolean,
    val showInfoIcon: Boolean,
    val onRowClick: () -> Unit,
    val onSuggestionActionClick: () -> Unit,
    val onInfoClick: () -> Unit
)

fun SuggestionViewModel.toSearchResultRowUiModel(): SearchResultRowUiModel {
    val tintIcon = shouldTintIcon()
    val resolvedIcon = icon.get()
        ?.constantState
        ?.newDrawable()
        ?.mutate()
        ?.apply {
            if (!tintIcon) {
                clearColorFilter()
            }
        } ?: icon.get()

    return SearchResultRowUiModel(
        title = title,
        subtitle = subtitle,
        matcher = term,
        titleTextColor = titleTextColor,
        subtitleTextColor = subtitleTextColor,
        icon = resolvedIcon,
        shouldTintIcon = tintIcon,
        showTimetableIcon = showTimetableIcon,
        showInfoIcon = showInfoIcon,
        onRowClick = { onItemClicked.perform() },
        onSuggestionActionClick = { onSuggestionActionClicked.perform() },
        onInfoClick = { onInfoClicked.perform() }
    )
}

private fun SuggestionViewModel.shouldTintIcon(): Boolean {
    val isHomeOrWork = when (this) {
        is FixedSuggestionViewModel -> {
            val enumName = (id as? Enum<*>)?.name
            enumName == "HOME" || enumName == "WORK" ||
                suggestion.location()?.locationType == Location.TYPE_HOME ||
                suggestion.location()?.locationType == Location.TYPE_WORK
        }
        is HistorySearchProviderSuggestionViewModel -> {
            suggestion.location()?.locationType == Location.TYPE_HOME ||
                suggestion.location()?.locationType == Location.TYPE_WORK
        }
        is GoogleAndTripGoSuggestionViewModel -> {
            location.locationType == Location.TYPE_HOME || location.locationType == Location.TYPE_WORK
        }
        else -> false
    }
    val isCurrentLocation = this is FixedSuggestionViewModel &&
        this.id.toString() == FixedSuggestions.CURRENT_LOCATION.name
    return !isHomeOrWork && !isCurrentLocation
}
