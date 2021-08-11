package com.skedgo.tripkit.ui.dialog

data class GenericListItem(
        val label: String,
        var selected: Boolean
) {
    companion object {
        fun parse(stringsForList: List<String>): List<GenericListItem> {
            return stringsForList.map {
                GenericListItem(label = it, selected = false)
            }
        }
    }
}