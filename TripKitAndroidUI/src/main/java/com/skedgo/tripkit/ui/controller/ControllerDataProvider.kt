package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewFixedSuggestionsProvider
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandlerFactory

object ControllerDataProvider {

    var suggestionProvider: TKUIHomeViewFixedSuggestionsProvider? = null
    var favoriteProvider: TKUIFavoritesSuggestionProvider? = null
    var actionButtonHandlerFactory: TKUIActionButtonHandlerFactory? = null

    fun getFavoritesHome(): Location? = favoriteProvider?.getHome()
    fun getFavoritesWork(): Location? = favoriteProvider?.getWork()
}