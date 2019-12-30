package com.skedgo.tripkit.ui.core.module
import com.skedgo.tripkit.ui.timetables.ServiceRepository
import com.skedgo.tripkit.ui.timetables.ServiceRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class ServiceDetailsModule {
  @Provides
  fun serviceRepository(repository: ServiceRepositoryImpl): ServiceRepository = repository
}