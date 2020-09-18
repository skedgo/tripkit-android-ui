package com.skedgo.tripkit.ui.routing.settings
import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

internal class PrioritiesRepositoryImpl @Inject constructor(
    resources: Resources,
    private val prefs: SharedPreferences
) : PrioritiesRepository {
  private val defaultValue = 50
  private val budgetPriorityKey = resources.getString(R.string.pref_budget)
  private val timePriorityKey = resources.getString(R.string.pref_time)
  private val environmentPriorityKey = resources.getString(R.string.pref_carbon)
  private val conveniencePriorityKey = resources.getString(R.string.pref_hassle)
  private val exercisePriorityKey = resources.getString(R.string.pref_exercise)

  override fun getBudgetPriority(): Observable<Priority.Budget> =
      Observable.fromCallable {
        Priority.Budget(prefs.getInt(budgetPriorityKey, defaultValue))
      }

  override fun putBudgetPriority(budgetPriority: Priority.Budget) = prefs.edit().putInt(budgetPriorityKey, budgetPriority.value).apply()


  override fun getTimePriority(): Observable<Priority.Time> =
      Observable.fromCallable {
        Priority.Time(prefs.getInt(timePriorityKey, defaultValue))
      }

  override fun putTimePriority(timePriority: Priority.Time) = prefs.edit().putInt(timePriorityKey, timePriority.value).apply()

  override fun getEnvironmentPriority(): Observable<Priority.Environment> =
      Observable.fromCallable {
        Priority.Environment(prefs.getInt(environmentPriorityKey, defaultValue))
      }

  override fun putEnvironmentPriority(environmentPriority: Priority.Environment) = prefs.edit().putInt(environmentPriorityKey, environmentPriority.value).apply()

  override fun getConveniencePriority(): Observable<Priority.Convenience> =
      Observable.fromCallable {
        Priority.Convenience(prefs.getInt(conveniencePriorityKey, defaultValue))
      }

  override fun putConveniencePriority(conveniencePriority: Priority.Convenience)= prefs.edit().putInt(conveniencePriorityKey, conveniencePriority.value).apply()
  override fun getExercisePriority(): Observable<Priority.Exercise> =
      Observable.fromCallable {
        Priority.Exercise(prefs.getInt(exercisePriorityKey, defaultValue))
      }

  override fun putExercisePriority(exercisePriority: Priority.Exercise) = prefs.edit().putInt(exercisePriorityKey, exercisePriority.value).apply()
}