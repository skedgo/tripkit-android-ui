package com.skedgo.tripkit.ui.base

import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.concurrent.TimeUnit

/**
 * Will serve as a base class for test classes using MockK
 */
@OptIn(ExperimentalCoroutinesApi::class)
open class MockKTest {

    val testDispatcher = StandardTestDispatcher()

    val immediateScheduler: Scheduler = object : Scheduler() {

        override fun createWorker() = ExecutorScheduler.ExecutorWorker { it.run() }

        // This prevents errors when scheduling a delay
        override fun scheduleDirect(run: Runnable, delay: Long, unit: TimeUnit): Disposable {
            return super.scheduleDirect(run, 0, unit)
        }

    }

    fun initDispatchers() {
        Dispatchers.setMain(testDispatcher)
    }

    fun tearDownDispatchers() {
        Dispatchers.resetMain()
    }

    fun initRx() {
        RxJavaPlugins.setIoSchedulerHandler { immediateScheduler }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediateScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { immediateScheduler }
    }

    fun tearDownRx() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

}