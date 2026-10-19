package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.routing.settings.RemindersRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemindersRepositoryModule {
    @Binds
    internal abstract fun remindersRepository(impl: RemindersRepositoryImpl): RemindersRepository
}
