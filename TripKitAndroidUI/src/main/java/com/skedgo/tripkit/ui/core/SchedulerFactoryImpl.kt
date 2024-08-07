package com.skedgo.tripkit.ui.core

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.computation
import io.reactivex.schedulers.Schedulers.io
import javax.inject.Inject

class SchedulerFactoryImpl @Inject internal constructor() : SchedulerFactory {
    override val mainScheduler: Scheduler = mainThread()
    override val ioScheduler: Scheduler = io()
    override val computationScheduler: Scheduler = computation()
}
