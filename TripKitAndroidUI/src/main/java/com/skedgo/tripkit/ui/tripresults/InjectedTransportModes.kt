package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.common.model.TransportMode

/**
 * Small registry for transport modes that are injected by the app/UI layer (not by region backend).
 *
 * How it works:
 * 1) UI injection:
 *    `mergeWithInjectedModes(...)` combines backend modes with injected modes so the mode is visible
 *    and toggleable in Transport Selector + Trip Results transport chips.
 * 2) Request behavior:
 *    `shouldIncludeBackendMode(...)` maps a selected injected mode into real backend mode ids used by routing.
 *    Example: Park & Ride selected -> include both Public Transport + Car in route request filtering.
 *
 * This keeps custom mode behavior centralized so adding another custom mode is mostly adding one config entry.
 */
data class InjectedTransportMode(
    val identifier: String,
    val title: String,
    val subtitle: String? = null,
    val iconId: String,
    val supportsDetails: Boolean = false,
    val effectiveRoutingModes: Set<String> = emptySet()
)

object InjectedTransportModes {
    const val ID_PARK_RIDE = "park_ride"

    // Add future injected/custom modes here.
    private val injectedModes: List<InjectedTransportMode> = listOf(
        InjectedTransportMode(
            identifier = ID_PARK_RIDE,
            title = "Park & Ride",
            iconId = "ic_park_ride",
            supportsDetails = false,
            effectiveRoutingModes = setOf(
                TransportMode.ID_PUBLIC_TRANSPORT,
                TransportMode.ID_CAR
            )
        )
    )

    fun all(): List<InjectedTransportMode> = injectedModes

    fun findById(modeId: String?): InjectedTransportMode? =
        injectedModes.firstOrNull { it.identifier == modeId }

    // Used by UI view models to expose injected modes in the same list as backend modes.
    fun mergeWithInjectedModes(baseModes: List<TransportMode>): List<TransportMode> {
        val byId = LinkedHashMap<String, TransportMode>()
        baseModes.forEach { mode ->
            byId[mode.id] = mode
        }
        injectedModes.forEach { mode ->
            if (!byId.containsKey(mode.identifier)) {
                byId[mode.identifier] = TransportMode(
                    id = mode.identifier,
                    title = mode.title,
                    iconId = mode.iconId
                )
            }
        }
        return byId.values.toList()
    }

    // Used by routing filter to translate selected injected mode -> effective backend mode ids.
    fun shouldIncludeBackendMode(
        backendModeId: String,
        isInjectedModeSelected: (String) -> Boolean
    ): Boolean {
        return injectedModes.any { mode ->
            isInjectedModeSelected(mode.identifier) &&
                mode.effectiveRoutingModes.contains(backendModeId)
        }
    }
}
