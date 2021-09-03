package com.skedgo.tripkit.ui.map.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.common.model.SharedVehicleType
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingVehicleEntity
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.BikePodPOILocation
import com.skedgo.tripkit.ui.map.FreeFloatingVehiclePOILocation
import com.skedgo.tripkit.ui.trippreview.nearby.title

class FreeFloatingVehicleInfoWindowAdapter(private val context: Context) : StopInfoWindowAdapter {
    val view: View by lazy { LayoutInflater.from(context).inflate(R.layout.free_floating_vehicle_info_window, null, false) }

    @SuppressLint("InflateParams")
    override fun getInfoWindow(marker: Marker): View {
        val entity = (marker.tag as FreeFloatingVehiclePOILocation).freeFloatingLocationEntity.vehicle
        view.findViewById<TextView>(R.id.operatorName).text = entity.operator.name
        val vehicleType = entity.sharedVehicleType()
        val vehicleTypeView = view.findViewById<TextView>(R.id.type)
        vehicleTypeView.text = context.getString(vehicleType.title())
        vehicleTypeView.setCompoundDrawablesWithIntrinsicBounds(vehicleType.iconId, 0, 0, 0)
        view.findViewById<TextView>(R.id.name).text = entity.name
        view.findViewById<TextView>(R.id.batteryLevel).text = String.format("%d%%", entity.batteryLevel)
        view.findViewById<TextView>(R.id.batteryLevelText).setCompoundDrawablesWithIntrinsicBounds(
                when {
                    entity.batteryLevel < 12 -> R.drawable.ic_battery_0
                    entity.batteryLevel < 37 -> R.drawable.ic_battery_25
                    entity.batteryLevel < 62 -> R.drawable.ic_battery_50
                    entity.batteryLevel < 87 -> R.drawable.ic_battery_75
                    else -> R.drawable.ic_battery_100
                }, 0, 0, 0)
        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun onInfoWindowClosed(marker: Marker) {}

    override fun windowInfoHeightInPixel(marker: Marker): Int = view.height
}

fun FreeFloatingVehicleEntity.sharedVehicleType(): SharedVehicleType {
    var vehicleType = this.vehicleType
    this.vehicleTypeInfo?.let {
        vehicleType = it.formFactor
    }

    val sharedVehicleType = SharedVehicleType.valueOf(vehicleType)
    return if (sharedVehicleType == SharedVehicleType.SCOOTER) {
        SharedVehicleType.KICK_SCOOTER
    } else {
        sharedVehicleType
    }
}


