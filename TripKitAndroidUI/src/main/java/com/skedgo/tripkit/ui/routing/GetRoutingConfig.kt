package com.skedgo.tripkit.ui.routing
import io.reactivex.Observable

interface GetRoutingConfig {
  fun execute(): Observable<RoutingConfig>
}