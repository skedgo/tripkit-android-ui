package com.skedgo.tripkit.ui.tripresult

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import java.time.Duration

internal class PreferredTransferTimeRepositoryImpl constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : PreferredTransferTimeRepository {
    override suspend fun putPreferredTransferTime(preferredTransferTime: Duration) {
        // FIXME: Should use this Repository to change preferred transfer time.
        TODO("Not implemented yet.")
    }

    override suspend fun getPreferredTransferTime(
        defaultIfEmpty: () -> Duration
    ): Duration {
        val transferTime =
            prefs.getString(resources.getString(R.string.pref_transfer_time), null)?.toLong()
        return if (transferTime != null) {
            Duration.ofMinutes(transferTime)
        } else {
            defaultIfEmpty()
        }
    }

}