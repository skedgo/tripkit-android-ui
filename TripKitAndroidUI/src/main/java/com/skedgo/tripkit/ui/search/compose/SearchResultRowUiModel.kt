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
    val id: String,
    val title: String,
    val subtitle: String?,
    val matcher: String?,
    val titleTextColor: Int,
    val subtitleTextColor: Int,
    val icon: Drawable?,
    val shouldTintIcon: Boolean,
    val groupKind: SearchResultGroupKind,
    val isFirstInGroup: Boolean,
    val isLastInGroup: Boolean,
    val showGroupTopSpacing: Boolean,
    val showTimetableIcon: Boolean,
    val showInfoIcon: Boolean,
    val onRowClick: () -> Unit,
    val onSuggestionActionClick: () -> Unit,
    val onInfoClick: () -> Unit
)

enum class SearchResultGroupKind {
    FIXED,
    HISTORY,
    GOOGLE_AND_TRIPGO
}

private data class GroupedSuggestion(
    val item: SuggestionViewModel,
    val groupKind: SearchResultGroupKind
)

fun mapSearchResultRows(items: List<SuggestionViewModel>): List<SearchResultRowUiModel> {
    val fixed = items.filterIsInstance<FixedSuggestionViewModel>().map {
        GroupedSuggestion(it, SearchResultGroupKind.FIXED)
    }
    val history = items.filterIsInstance<HistorySearchProviderSuggestionViewModel>().map {
        GroupedSuggestion(it, SearchResultGroupKind.HISTORY)
    }
    val googleAndTripGo = items
        .filter { it !is FixedSuggestionViewModel && it !is HistorySearchProviderSuggestionViewModel }
        .map { GroupedSuggestion(it, SearchResultGroupKind.GOOGLE_AND_TRIPGO) }

    return buildList {
        addAll(groupToRows(fixed, showTopSpacing = false))
        addAll(groupToRows(history, showTopSpacing = fixed.isNotEmpty()))
        addAll(
            groupToRows(
                googleAndTripGo,
                showTopSpacing = fixed.isNotEmpty() || history.isNotEmpty()
            )
        )
    }
}

fun SuggestionViewModel.toSearchResultRowUiModel(): SearchResultRowUiModel {
    val groupKind = when (this) {
        is FixedSuggestionViewModel -> SearchResultGroupKind.FIXED
        is HistorySearchProviderSuggestionViewModel -> SearchResultGroupKind.HISTORY
        else -> SearchResultGroupKind.GOOGLE_AND_TRIPGO
    }
    return toSearchResultRowUiModel(
        groupKind = groupKind,
        isFirstInGroup = true,
        isLastInGroup = true,
        showGroupTopSpacing = false
    )
}

private fun groupToRows(
    groupedItems: List<GroupedSuggestion>,
    showTopSpacing: Boolean
): List<SearchResultRowUiModel> {
    if (groupedItems.isEmpty()) return emptyList()
    return groupedItems.mapIndexed { index, grouped ->
        grouped.item.toSearchResultRowUiModel(
            groupKind = grouped.groupKind,
            isFirstInGroup = index == 0,
            isLastInGroup = index == groupedItems.lastIndex,
            showGroupTopSpacing = showTopSpacing && index == 0
        )
    }
}

private fun SuggestionViewModel.toSearchResultRowUiModel(
    groupKind: SearchResultGroupKind,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    showGroupTopSpacing: Boolean
): SearchResultRowUiModel {
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
        id = "${groupKind.name}:${System.identityHashCode(this)}:$title:${subtitle.orEmpty()}",
        title = title,
        subtitle = subtitle,
        matcher = term,
        titleTextColor = titleTextColor,
        subtitleTextColor = subtitleTextColor,
        icon = resolvedIcon,
        shouldTintIcon = tintIcon,
        groupKind = groupKind,
        isFirstInGroup = isFirstInGroup,
        isLastInGroup = isLastInGroup,
        showGroupTopSpacing = showGroupTopSpacing,
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
