package com.skedgo.tripkit.ui.data.cameraposition
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.camera.MapCameraPosition

fun CameraPosition.toMapCameraPosition(): MapCameraPosition =
    MapCameraPosition(this.target.latitude, this.target.longitude,
        this.zoom, this.tilt, this.bearing)

fun MapCameraPosition.toCameraPosition(): CameraPosition =
    CameraPosition(LatLng(this.lat, this.lng),
        this.zoom, this.tilt, this.bearing)