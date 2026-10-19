package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routing.settings.PrioritiesRepository
import com.skedgo.tripkit.ui.routing.settings.PrioritiesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PrioritiesRepositoryModule {
    @Binds
    internal abstract fun prioritiesRepository(
        impl: PrioritiesRepositoryImpl
    ): PrioritiesRepository
}
