package com.skedgo.tripkit.ui.routing.settings
import io.reactivex.Completable
import io.reactivex.Observable

interface PrioritiesRepository {
  fun getBudgetPriority(): Observable<Priority.Budget>
  fun putBudgetPriority(budgetPriority: Priority.Budget)
  fun getTimePriority(): Observable<Priority.Time>
  fun putTimePriority(timePriority: Priority.Time)
  fun getEnvironmentPriority(): Observable<Priority.Environment>
  fun putEnvironmentPriority(environmentPriority: Priority.Environment)
  fun getConveniencePriority(): Observable<Priority.Convenience>
  fun putConveniencePriority(conveniencePriority: Priority.Convenience)
  fun getExercisePriority(): Observable<Priority.Exercise>
  fun putExercisePriority(exercisePriority: Priority.Exercise)
}