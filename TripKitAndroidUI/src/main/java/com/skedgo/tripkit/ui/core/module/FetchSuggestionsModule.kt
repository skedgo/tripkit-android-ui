package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.geocoding.AutoCompleteTask
import com.skedgo.tripkit.ui.search.FetchSuggestions
import dagger.Module
import dagger.Provides

@Module
class FetchSuggestionsModule {
    @Provides
    fun fetchSuggestions(autoCompleteTask: AutoCompleteTask): FetchSuggestions {
        return autoCompleteTask
    }
}
