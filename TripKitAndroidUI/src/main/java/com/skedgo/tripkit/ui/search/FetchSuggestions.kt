package com.skedgo.tripkit.ui.search

import io.reactivex.Observable

interface FetchSuggestions {
    fun query(parameters: FetchLocationsParameters): Observable<com.skedgo.tripkit.ui.geocoding.AutoCompleteResult>
}