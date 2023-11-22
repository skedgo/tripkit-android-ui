package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.routing.settings.RemindersRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class RemindersRepositoryModule {
    @Binds
    internal abstract fun remindersRepository(impl: RemindersRepositoryImpl): RemindersRepository
}
