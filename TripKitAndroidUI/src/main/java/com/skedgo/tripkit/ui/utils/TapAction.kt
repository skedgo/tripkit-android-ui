package com.skedgo.tripkit.ui.utils

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TapAction<TSender> internal constructor(
    private val getSender: () -> TSender
) {
  companion object Factory {
    fun <TSender> create(getSender: () -> TSender): TapAction<TSender>
        = TapAction(getSender)
  }

  private val onTap = PublishSubject.create<TSender>()
  val observable: Observable<TSender>
    get() = onTap.hide()

  fun perform() = onTap.onNext(getSender())
}