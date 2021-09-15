package com.skedgo.tripkit.ui.dialog

import com.skedgo.tripkit.booking.quickbooking.Option
import org.joda.time.format.ISODateTimeFormat
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

data class GenericListItem(
        val label: String,
        val subLabel: String?,
        val date: String?,
        var selected: Boolean,
        val itemId: String? = null
) {
    companion object {
        fun parseStrings(stringsForList: List<String>): List<GenericListItem> {
            return stringsForList.map {
                GenericListItem(label = it, null, null, selected = false)
            }
        }

        fun parseOptions(options: List<Option>): List<GenericListItem> {
            return options.map {
                var formattedDate: String? = null
                it.timestamp?.let { timestamp ->
                    val fromSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK)
                    val toSdfDate = SimpleDateFormat("MMM d, yyyy", Locale.UK)
                    val toSdfTime = SimpleDateFormat("h:mm aa", Locale.UK)
                    val date = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(timestamp)
                    date?.let { _date ->
                        val _formattedDate = toSdfDate.format(_date.toDate())
                        val _formattedTime = toSdfTime.format(_date.toDate())
                        formattedDate = String.format("%s at %s", _formattedDate, _formattedTime)
                    }
                }
                GenericListItem(label = it.title, subLabel = it.provider, date = formattedDate, selected = false, itemId = it.id)
            }
        }
    }
}