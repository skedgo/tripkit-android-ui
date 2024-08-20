package com.skedgo.tripkit.ui.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.skedgo.tripkit.configuration.ServerManager
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import io.reactivex.Observable

class DeveloperPreferenceRepositoryImpl constructor(
    private val context: Context,
    private val preferences: SharedPreferences
) : DeveloperPreferenceRepository {
    private val isEnabledKey by lazy { "isEnabled" }
    private val bsbKey by lazy { context.getString(R.string.pref_bsb) }
    private val psbKey by lazy { context.getString(R.string.pref_psb) }
    private val wfwKey by lazy { context.getString(R.string.pref_wfw) }
    private val serverTypeKey by lazy { context.getString(R.string.pref_server_type) }
    private val customServerKey by lazy { context.getString(R.string.pref_custom_server) }
    private val productionServer by lazy { "" }
    private val betaServer by lazy { ServerManager.configuration.bigBangUrl }

    override val onIsEnabledChange: Observable<Boolean> by lazy {
        preferences.onChange(isEnabledKey)
            .map { it.first.getBoolean(it.second, false) }
    }

    override val onServerChange: Observable<String> by lazy {
        val onServerTypeChange = preferences.onChange(serverTypeKey)
        val onCustomServerChange = preferences.onChange(customServerKey)
            .filter { serverType() == context.getString(R.string.pref_custom_server) }
        Observable.merge(onServerTypeChange, onCustomServerChange)
            .map { server }
    }

    override var isEnabled: Boolean
        get() {
            return preferences.getBoolean(isEnabledKey, false)
        }
        set(value) {
            preferences.edit()
                .putBoolean(isEnabledKey, value)
                .apply()
        }

    override var bookingsUseSandbox: Boolean
        get() {
            return preferences.getBoolean(bsbKey, false)
        }
        set(value) {
            preferences.edit()
                .putBoolean(bsbKey, value)
                .apply()
        }

    override var paymentsUseSandbox: Boolean
        get() {
            return preferences.getBoolean(psbKey, false)
        }
        set(value) {
            preferences.edit().putBoolean(psbKey, value).apply()
        }
    override var wayFinderWikiEnabled: Boolean
        get() {
            return preferences.getBoolean(wfwKey, false)
        }
        set(value) {
            preferences.edit().putBoolean(wfwKey, value).apply()
        }

    override val server: String
        get() {
            val serverType = serverType()
            when (serverType) {
                context.getString(R.string.pref_production_server) -> return productionServer
                context.getString(R.string.pref_beta_server) -> return betaServer
                context.getString(R.string.pref_custom_server) -> return preferences.getString(
                    serverType,
                    productionServer
                )!!
                else -> throw IllegalStateException("Unknown server type: $serverType")
            }
        }

    private fun serverType() = preferences.getString(
        serverTypeKey,
        if (BuildConfig.DEBUG) {
            context.getString(R.string.pref_beta_server)
        } else {
            context.getString(R.string.pref_production_server)
        }
    )
}
