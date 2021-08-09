package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.analytics.UserInfoRepository
import com.skedgo.tripkit.analytics.UserInfoRepositoryImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UserInfoRepositoryModule {
    @Provides
    @Singleton
    fun userInfoRepository(): UserInfoRepository = UserInfoRepositoryImpl()
}