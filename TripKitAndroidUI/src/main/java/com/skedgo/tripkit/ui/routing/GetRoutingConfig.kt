package com.skedgo.tripkit.ui.routing

interface GetRoutingConfig {

  suspend fun execute(): RoutingConfig
}