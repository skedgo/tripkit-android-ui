package com.skedgo.tripkit.ui.routing.settings

import com.skedgo.tripkit.ui.routing.settings.Priority.*

data class WeightingProfile(
    val budgetPriority: Budget = Budget(),
    val environmentPriority: Environment = Environment(),
    val timePriority: Time = Time(),
    val conveniencePriority: Convenience = Convenience(),
    val exercisePriority: Exercise = Exercise()
)