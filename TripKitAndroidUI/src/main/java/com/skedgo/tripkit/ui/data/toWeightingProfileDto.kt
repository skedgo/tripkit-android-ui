package com.skedgo.tripgo.sdk.agenda.data

import com.skedgo.tripkit.a2brouting.toProfileWeight
import com.skedgo.tripkit.ui.routing.settings.WeightingProfile

fun WeightingProfile.toWeightingProfileDto(): String {
  val price = budgetPriority.value.toProfileWeight()
  val environment = environmentPriority.value.toProfileWeight()
  val duration = timePriority.value.toProfileWeight()
  val convenience = conveniencePriority.value.toProfileWeight()
  return "($price,$environment,$duration,$convenience)"
}