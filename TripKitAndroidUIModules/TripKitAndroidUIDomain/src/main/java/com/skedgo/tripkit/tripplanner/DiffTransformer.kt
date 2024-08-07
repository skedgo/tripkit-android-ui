package com.skedgo.tripkit.tripplanner

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single

class DiffTransformer<T, R>(
    val keySelector: (T) -> String,
    val createMarkerOptions: (T) -> Single<R>
) : ObservableTransformer<List<T>, Pair<List<Pair<R, T>>, Set<String>>> {

    override fun apply(o: Observable<List<T>>): Observable<Pair<List<Pair<R, T>>, Set<String>>> {
        return o.scan(Triple(emptyList<T>(), emptyList<T>(), emptyList<T>()),
            { oldResult: Triple<List<T>, List<T>, List<T>>, newList: List<T> ->
                val newSet = newList.associateBy(keySelector)
                val oldSet = oldResult.third.associateBy(keySelector)
                val insertion = newList.map(keySelector)
                    .subtract(oldResult.third.map { keySelector.invoke(it) }).map { newSet[it]!! }
                val removal = oldResult.third.map(keySelector)
                    .subtract(newList.map { keySelector.invoke(it) }).map { oldSet[it]!! }
                Triple(insertion, removal, newList)
            })
            .skip(1)
            .map { it.first to it.second }
            .flatMap { pair ->
                Observable.fromIterable(pair.first)
                    .flatMapSingle { t ->
                        createMarkerOptions(t).map { it to t }
                    }
                    .toList().toObservable()
                    .map { it to pair.second.map(keySelector).toSet() }
            }
    }
}