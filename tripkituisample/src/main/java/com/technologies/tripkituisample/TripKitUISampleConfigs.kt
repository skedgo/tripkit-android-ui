package com.technologies.tripkituisample

import androidx.annotation.Nullable
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import org.immutables.value.Value

@Value.Immutable
abstract class TripKitUISampleConfigs: Configs {
    @Nullable
    abstract fun actionButtonHandlerFactory(): ActionButtonHandlerFactory?

    companion object {
        fun builder(): ImmutableTripKitUISampleConfigs.Builder {
            return ImmutableTripKitUISampleConfigs.builder()
        }
    }
}