package com.skedgo.tripkit.ui.search

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import com.skedgo.tripkit.common.model.Location

interface SearchSuggestion {
    fun id(): Any
    fun title(): String
    fun subtitle(): String?

    @ColorRes
    fun titleColor(): Int
    @ColorRes fun subtitleColor(): Int

    fun icon(): Drawable
    fun location(): Location?
}

class DefaultSearchSuggestion(val id: Any,
                              val title: String,
                              val subtitle: String?,
                              @ColorRes val titleColor: Int,
                              @ColorRes val subtitleColor: Int,
                              val icon: Drawable,
                                val location: Location? = null): SearchSuggestion {
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

    override fun location(): Location? {
        return this.location
    }
}
