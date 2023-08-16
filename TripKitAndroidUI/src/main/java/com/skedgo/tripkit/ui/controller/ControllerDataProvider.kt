package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewFixedSuggestionsProvider
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider

object ControllerDataProvider {

    var suggestionProvider: TKUIHomeViewFixedSuggestionsProvider? = null
    var favoriteProvider: TKUIFavoritesSuggestionProvider? = null

}