package com.skedgo.tripkit.ui.routing.settings
import io.reactivex.Completable
import io.reactivex.Observable

interface PrioritiesRepository {
  suspend fun getBudgetPriority(): Priority.Budget
  suspend fun putBudgetPriority(budgetPriority: Priority.Budget)
  suspend fun getTimePriority(): Priority.Time
  suspend fun putTimePriority(timePriority: Priority.Time)
  suspend fun getEnvironmentPriority(): Priority.Environment
  suspend fun putEnvironmentPriority(environmentPriority: Priority.Environment)
  suspend fun getConveniencePriority(): Priority.Convenience
  suspend fun putConveniencePriority(conveniencePriority: Priority.Convenience)
  suspend fun getExercisePriority(): Priority.Exercise
  suspend fun putExercisePriority(exercisePriority: Priority.Exercise)
}