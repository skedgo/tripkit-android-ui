package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.core.SchedulerFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerFactoryModule {
    @Binds
    @Singleton
    abstract fun schedulerFactory(impl: SchedulerFactoryImpl): SchedulerFactory
}
