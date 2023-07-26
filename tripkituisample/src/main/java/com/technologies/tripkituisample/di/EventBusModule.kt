package com.technologies.tripkituisample.di

import com.technologies.tripkituisample.AppEventBus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class EventBusModule() {
    @Provides
    @Singleton
    fun tripGoEventBus() = AppEventBus

}