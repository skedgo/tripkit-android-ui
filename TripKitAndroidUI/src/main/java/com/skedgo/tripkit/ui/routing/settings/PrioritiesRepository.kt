package com.skedgo.tripkit.ui.routing.settings
import io.reactivex.Completable
import io.reactivex.Observable

interface PrioritiesRepository {
  fun getBudgetPriority(): Observable<Priority.Budget>
  fun putBudgetPriority(budgetPriority: Priority.Budget): Completable
  fun getTimePriority(): Observable<Priority.Time>
  fun putTimePriority(timePriority: Priority.Time): Completable
  fun getEnvironmentPriority(): Observable<Priority.Environment>
  fun putEnvironmentPriority(environmentPriority: Priority.Environment): Completable
  fun getConveniencePriority(): Observable<Priority.Convenience>
  fun putConveniencePriority(conveniencePriority: Priority.Convenience): Completable
  fun getExercisePriority(): Observable<Priority.Exercise>
  fun putExercisePriority(exercisePriority: Priority.Exercise): Completable
}