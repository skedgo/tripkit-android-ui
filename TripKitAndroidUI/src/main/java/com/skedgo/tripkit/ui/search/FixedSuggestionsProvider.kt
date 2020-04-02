package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.ui.R

interface FixedSuggestion {
    fun id(): Any
    fun title(): String
    fun subtitle(): String?

    @ColorRes fun titleColor(): Int
    @ColorRes fun subtitleColor(): Int

    fun icon(): Drawable
}

interface FixedSuggestionsProvider {
    fun fixedSuggestions(context: Context, iconProvider: LocationSearchIconProvider): List<FixedSuggestion>
}

class DefaultFixedSuggestion(val id: Any,
                             val title: String,
                             val subtitle: String?,
                             @ColorRes val titleColor: Int,
                             @ColorRes val subtitleColor: Int,
                             val icon: Drawable): FixedSuggestion {
    override fun id(): Any {
        return this.id
    }
    override fun title(): String {
        return this.title
    }

    override fun subtitle(): String? {
        return this.subtitle
    }

    override fun titleColor(): Int {
        return this.titleColor
    }

    override fun subtitleColor(): Int {
        return this.subtitleColor
    }

    override fun icon(): Drawable {
        return this.icon
    }
}

enum class DefaultFixedSuggestionType {
    CURRENT_LOCATION,
    CHOOSE_ON_MAP
}

class DefaultFixedSuggestionsProvider(val showCurrentLocation: Boolean, val showChooseOnMap: Boolean) : FixedSuggestionsProvider {
    override fun fixedSuggestions(context: Context, iconProvider: LocationSearchIconProvider): List<FixedSuggestion> {
        val currentLocation = DefaultFixedSuggestion(DefaultFixedSuggestionType.CURRENT_LOCATION, context.getString(R.string.current_location),
                                                     null,
                                                     R.color.title_text,
                                                     R.color.description_text,
                                                     ContextCompat.getDrawable(context,
                                                             iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CURRENT_LOCATION))!! )
        val chooseOnMap = DefaultFixedSuggestion(DefaultFixedSuggestionType.CHOOSE_ON_MAP, context.getString(R.string.choose_on_map),
                null,
                R.color.title_text,
                R.color.description_text,
                ContextCompat.getDrawable(context,
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.DROP_PIN))!! )

        val list = mutableListOf<FixedSuggestion>()
        if (showCurrentLocation) {
            list.add(currentLocation)
        }

        if (showChooseOnMap) {
            list.add(chooseOnMap)
        }
        return list
    }

}