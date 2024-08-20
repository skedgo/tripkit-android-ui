package com.skedgo.tripkit.ui.core.module

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.skedgo.tripkit.ui.data.personaldata.MyPersonalDataRepositoryImpl
import com.skedgo.tripkit.ui.personaldata.MyPersonalDataRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class MyPersonalDataModule {
    @Provides
    internal fun myPersonalDataRepository(repositoryImpl: MyPersonalDataRepositoryImpl): MyPersonalDataRepository {
        return repositoryImpl
    }

    @Provides
    @Named("MyPersonalData")
    fun myPersonalDataSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("MyPersonalData", MODE_PRIVATE)
    }
}