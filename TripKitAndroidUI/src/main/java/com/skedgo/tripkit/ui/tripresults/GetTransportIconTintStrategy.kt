package com.skedgo.tripkit.ui.tripresults

import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import io.reactivex.Single
import javax.inject.Inject

open class GetTransportIconTintStrategy @Inject constructor(private val resources: Resources) {
    open operator fun invoke(): Single<TransportTintStrategy> =
        Single.fromCallable {
            when (resources.getBoolean(R.bool.trip_kit_use_service_color)) {
                true -> ApplyTintStrategy
                false -> NoTintStrategy
            }
        }
}
