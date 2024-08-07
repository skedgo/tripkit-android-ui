package com.skedgo.tripkit.ui.core

import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.BuildConfig
import timber.log.Timber
import javax.inject.Inject

class ErrorLoggerImpl @Inject internal constructor() : ErrorLogger {
    override fun trackError(error: Throwable) {
        logError(error)
    }

    override fun logError(error: Throwable) {
        if (BuildConfig.DEBUG) {
            Timber.e(error, "An error occurred")
        }
    }
}

