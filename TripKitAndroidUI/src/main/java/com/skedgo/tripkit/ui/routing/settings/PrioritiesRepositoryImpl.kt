package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
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

    override suspend fun getBudgetPriority(): Priority.Budget =
        Priority.Budget(prefs.getInt(budgetPriorityKey, defaultValue))

    override suspend fun putBudgetPriority(budgetPriority: Priority.Budget) =
        prefs.edit().putInt(budgetPriorityKey, budgetPriority.value).apply()

    override suspend fun getTimePriority(): Priority.Time =
        Priority.Time(prefs.getInt(timePriorityKey, defaultValue))

    override suspend fun putTimePriority(timePriority: Priority.Time) =
        prefs.edit().putInt(timePriorityKey, timePriority.value).apply()

    override suspend fun getEnvironmentPriority(): Priority.Environment =
        Priority.Environment(prefs.getInt(environmentPriorityKey, defaultValue))

    override suspend fun putEnvironmentPriority(environmentPriority: Priority.Environment) =
        prefs.edit().putInt(environmentPriorityKey, environmentPriority.value).apply()

    override suspend fun getConveniencePriority(): Priority.Convenience =
        Priority.Convenience(prefs.getInt(conveniencePriorityKey, defaultValue))

    override suspend fun putConveniencePriority(conveniencePriority: Priority.Convenience) =
        prefs.edit().putInt(conveniencePriorityKey, conveniencePriority.value).apply()

    override suspend fun getExercisePriority(): Priority.Exercise =
        Priority.Exercise(prefs.getInt(exercisePriorityKey, defaultValue))

    override suspend fun putExercisePriority(exercisePriority: Priority.Exercise) =
        prefs.edit().putInt(exercisePriorityKey, exercisePriority.value).apply()
}