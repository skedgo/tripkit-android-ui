package com.skedgo.tripkit.ui.routing.settings

import com.skedgo.tripkit.ui.routing.settings.Priority.Budget
import com.skedgo.tripkit.ui.routing.settings.Priority.Convenience
import com.skedgo.tripkit.ui.routing.settings.Priority.Environment
import com.skedgo.tripkit.ui.routing.settings.Priority.Exercise
import com.skedgo.tripkit.ui.routing.settings.Priority.Time

data class WeightingProfile(
    val budgetPriority: Budget = Budget(),
    val environmentPriority: Environment = Environment(),
    val timePriority: Time = Time(),
    val conveniencePriority: Convenience = Convenience(),
    val exercisePriority: Exercise = Exercise()
)