package com.skedgo.tripkit.ui.routing.settings


interface UnitsRepository {
    fun putUnit(unit: String)
    fun getUnit(): String
}