package com.skedgo.tripkit.ui.core

import io.reactivex.Scheduler

interface SchedulerFactory {
    val mainScheduler: Scheduler
    val ioScheduler: Scheduler
    val computationScheduler: Scheduler
}
