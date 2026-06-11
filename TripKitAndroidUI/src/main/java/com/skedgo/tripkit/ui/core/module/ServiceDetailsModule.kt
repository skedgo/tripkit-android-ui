package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.servicedetail.ServiceDetailRepository
import com.skedgo.tripkit.servicedetail.ServiceDetailRepositoryImpl
import com.skedgo.tripkit.ui.timetables.ServiceRepository
import com.skedgo.tripkit.ui.timetables.ServiceRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class ServiceDetailsModule {
    @Provides
    fun serviceDetailRepository(impl: ServiceDetailRepositoryImpl): ServiceDetailRepository = impl

    @Provides
    fun serviceRepository(repository: ServiceRepositoryImpl): ServiceRepository = repository
}