package com.skedgo.tripkit.ui.routing

import android.os.Parcel
import android.os.Parcelable
import com.skedgo.tripkit.TransportModeFilter
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.model.UserMode


class SimpleTransportModeFilter() : TransportModeFilter {
    private var transportModes: Set<String> = setOf()
    private var avoidTransportModes: Set<String> = setOf()
    private var replacementUserModes: List<UserMode> = listOf()

    fun setTransportModes(transportModes: Set<String>) {
        this.transportModes = transportModes
    }

    fun setTransportModesToAvoid(transportModesToAvoid: Set<String>) {
        this.avoidTransportModes = transportModesToAvoid
    }

    override fun useTransportMode(mode: String): Boolean {
        return when {
            transportModes.isEmpty() -> true
            else -> {
                // Wheelchair support comes with walking support.
                if (mode == TransportMode.ID_WHEEL_CHAIR && transportModes.contains(TransportMode.ID_WALK)) {
                    return true;
                } else {
                    transportModes.contains(mode)
                }
            }
        }
    }

    override fun avoidTransportMode(mode: String): Boolean {
        return when {
            avoidTransportModes.isEmpty() -> false
            else -> avoidTransportModes.contains(mode)
        }
    }

    fun replaceTransportModesWithUserModes(mode: List<UserMode>) {
        replacementUserModes = mode
    }

    override fun getFilteredMode(originalModes: List<String>): List<String> {
        val modeArray = ArrayList(originalModes)
        replacementUserModes.forEach {
            if (modeArray.contains(it.mode)) {
                modeArray.remove(it.mode)
                it.rules?.replaceWith?.let { list ->
                    modeArray.addAll(list)
                }
            }
        }
        return modeArray
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