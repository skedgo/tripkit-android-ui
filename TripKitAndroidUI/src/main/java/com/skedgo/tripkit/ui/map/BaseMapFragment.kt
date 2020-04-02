package com.skedgo.tripkit.ui.map

import android.os.Bundle
import android.view.View
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.Some
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.ui.core.AutoDisposable
import com.skedgo.tripkit.ui.core.afterMeasured
import com.skedgo.tripkit.ui.core.filterSome
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer

abstract class BaseMapFragment : SupportMapFragment() {
    protected val autoDisposable = AutoDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoDisposable.bindTo(this.lifecycle)
    }

  private val whenViewIsMeasured: BehaviorRelay<Optional<Unit>> = BehaviorRelay.create()
  private var subscription: CompositeDisposable = CompositeDisposable()

  /**
   * A safer point to retrieve [GoogleMap] as the callback is invoked
   * after fragment view is measured. This makes it safe to move or animate map with CameraUpdate.
   */
  fun whenSafeToUseMap(callback: Consumer<GoogleMap>) {
    subscription.add(
        Flowable
            .create(FlowableOnSubscribe<GoogleMap> {
              super.getMapAsync { map: GoogleMap ->
                it.onNext(map)
                it.onComplete()
              }
            }, BackpressureStrategy.LATEST)
            .toObservable()
            .delaySubscription(whenViewIsMeasured.filterSome())
            .subscribe(callback::accept)
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    subscription.add(
        view.afterMeasured().subscribe { _: Unit ->
          whenViewIsMeasured.accept(Some(Unit))
        }
    )
  }

  override fun onDestroyView() {
    subscription.clear()
    whenViewIsMeasured.accept(None)
    super.onDestroyView()
  }
}
