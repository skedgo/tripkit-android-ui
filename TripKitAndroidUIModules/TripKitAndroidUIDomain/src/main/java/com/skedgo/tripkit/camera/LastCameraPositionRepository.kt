package com.skedgo.tripkit.camera

import io.reactivex.Observable

interface LastCameraPositionRepository {
    fun putMapCameraPosition(mapCameraPosition: MapCameraPosition?): Observable<MapCameraPosition?>
    fun getMapCameraPosition(): Observable<MapCameraPosition>
    fun getMapCameraPositionByLocale(): Observable<MapCameraPosition>
    fun getDefaultMapCameraPosition(): Observable<MapCameraPosition>
    fun hasMapCameraPosition(): Boolean
}