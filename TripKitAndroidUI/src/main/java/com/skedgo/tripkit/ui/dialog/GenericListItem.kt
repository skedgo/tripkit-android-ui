package com.skedgo.tripkit.ui.dialog

import com.skedgo.tripkit.booking.quickbooking.Option

data class GenericListItem(
        val label: String,
        var selected: Boolean,
        val itemId: String? = null
) {
    companion object {
        fun parseStrings(stringsForList: List<String>): List<GenericListItem> {
            return stringsForList.map {
                GenericListItem(label = it, selected = false)
            }
        }

        fun parseOptions(options: List<Option>): List<GenericListItem> {
            return options.map {
                GenericListItem(label = it.title, selected = false, itemId = it.id)
            }
        }
    }
}