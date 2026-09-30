package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.analytics.UserInfoRepository
import com.skedgo.tripkit.analytics.UserInfoRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UserInfoRepositoryModule {
    @Provides
    @Singleton
    fun userInfoRepository(): UserInfoRepository = UserInfoRepositoryImpl()
}