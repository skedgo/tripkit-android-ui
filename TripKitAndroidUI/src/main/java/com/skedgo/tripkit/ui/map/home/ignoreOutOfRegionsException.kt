package com.skedgo.tripkit.ui.map.home
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.OutOfRegionsException
import io.reactivex.Observable

fun Observable<Region>.ignoreOutOfRegionsException(): Observable<Region> {
  return this.onErrorResumeNext { t: Throwable ->
    if (t is OutOfRegionsException) {
      Observable.empty()
    } else {
      Observable.error(t)
    }
  }
}