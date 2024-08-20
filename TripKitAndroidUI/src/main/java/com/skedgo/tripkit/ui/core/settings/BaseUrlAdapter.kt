package com.skedgo.tripkit.ui.core.settings

import java.util.concurrent.Callable

class BaseUrlAdapter constructor(
    private val developerPreferenceRepository: DeveloperPreferenceRepository
) : Callable<String> {
    override fun call(): String {
        val url = developerPreferenceRepository.server
        return url
    }
}
