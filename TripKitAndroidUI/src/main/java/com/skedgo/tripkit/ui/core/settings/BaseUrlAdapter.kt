package com.skedgo.tripkit.ui.core.settings

import java.util.concurrent.Callable
import javax.inject.Inject

class BaseUrlAdapter  constructor(
    private val developerPreferenceRepository: DeveloperPreferenceRepository
) : Callable<String> {
  override fun call(): String = developerPreferenceRepository.server
}
