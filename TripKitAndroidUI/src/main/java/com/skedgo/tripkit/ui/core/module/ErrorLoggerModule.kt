package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.core.ErrorLoggerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorLoggerModule {
    @Binds
    @Singleton
    abstract fun errorLogger(impl: ErrorLoggerImpl): ErrorLogger
}
