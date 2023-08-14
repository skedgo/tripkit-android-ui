package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewFixedSuggestionsProvider
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider

object ControllerDataProvider {

    private var suggestionProvider: TKUIHomeViewFixedSuggestionsProvider? = null
    private var favoriteProvider: TKUIFavoritesSuggestionProvider? = null

}