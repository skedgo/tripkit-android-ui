package com.skedgo.tripkit.ui.favorites.v2.data.network

import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2

data class FavoriteResponse(
    val favorites: List<FavoriteV2>
)