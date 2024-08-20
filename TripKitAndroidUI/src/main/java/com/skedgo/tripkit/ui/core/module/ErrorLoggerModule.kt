package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.core.ErrorLoggerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class ErrorLoggerModule {
    @Binds
    @Singleton
    abstract fun errorLogger(impl: ErrorLoggerImpl): ErrorLogger
}
