package com.skedgo.tripkit.ui.search

import android.content.Context


interface LocationSearchProvider {
    suspend fun onClick(id: Any)
    suspend fun query(context: Context, iconProvider: LocationSearchIconProvider, query: String): List<SearchSuggestion>
}
