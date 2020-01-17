package com.skedgo.tripkit.ui.routing

import android.os.Parcel
import android.os.Parcelable
import com.skedgo.tripkit.TransportModeFilter


class SimpleTransportModeFilter() : TransportModeFilter {
    private var transportModes: Set<String> = setOf()
    private var avoidTransportModes: Set<String> = setOf()

    fun setTransportModes(transportModes: Set<String>) {
        this.transportModes = transportModes
    }

    fun setTransportModesToAvoid(transportModesToAvoid: Set<String>) {
        this.avoidTransportModes = transportModesToAvoid
    }

    override fun useTransportMode(mode: String): Boolean {
        return when {
            transportModes.isEmpty() -> true
            else -> transportModes.contains(mode)
        }
    }

    override fun avoidTransportMode(mode: String): Boolean {
        return when {
            avoidTransportModes.isEmpty() -> false
            else -> avoidTransportModes.contains(mode)
        }
    }

    constructor(parcel: Parcel) : this() {
        var list = mutableListOf<String>()
         parcel.readStringList(list)
        this.transportModes = list.toSet()

        var avoidList = mutableListOf<String>()
        parcel.readStringList(avoidList)
        this.avoidTransportModes = avoidList.toSet()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(transportModes.toList())
        parcel.writeStringList(avoidTransportModes.toList())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SimpleTransportModeFilter> {
        override fun createFromParcel(parcel: Parcel): SimpleTransportModeFilter {
            return SimpleTransportModeFilter(parcel)
        }

        override fun newArray(size: Int): Array<SimpleTransportModeFilter?> {
            return arrayOfNulls(size)
        }
    }
}