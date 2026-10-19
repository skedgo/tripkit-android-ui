package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.geocoding.AutoCompleteTask
import com.skedgo.tripkit.ui.search.FetchSuggestions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class FetchSuggestionsModule {
    @Provides
    fun fetchSuggestions(autoCompleteTask: AutoCompleteTask): FetchSuggestions {
        return autoCompleteTask
    }
}
