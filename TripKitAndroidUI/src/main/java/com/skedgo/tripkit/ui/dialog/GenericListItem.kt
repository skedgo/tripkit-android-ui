package com.skedgo.tripkit.ui.dialog

import com.skedgo.tripkit.booking.quickbooking.Fare
import com.skedgo.tripkit.booking.quickbooking.Option
import com.skedgo.tripkit.booking.quickbooking.Rider
import org.joda.time.format.ISODateTimeFormat
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import java.util.Date
import com.skedgo.tripkit.ui.utils.SystemTimeFormatManager

data class GenericListItem(
    val label: String,
    val subLabel: String?,
    val date: String?,
    var selected: Boolean,
    val itemId: String? = null,
    val descriptionTitle: String? = null,
    val description: String? = null
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
                GenericListItem(
                    label = it.title,
                    subLabel = it.provider,
                    date = formattedDate,
                    selected = false,
                    itemId = it.id
                )
            }
        }

        fun parseRiders(riders: List<Rider>): List<GenericListItem> {
            return riders.map {
                GenericListItem(
                    it.name, null, null, selected = false, null, it.name, it.description
                )
            }
        }

        fun parseFares(fares: List<Fare>, rider: Rider?): List<GenericListItem> {
            rider?.let {
                val filteredTickets = fares.filter { ticket ->
                    ticket.riders.contains(it)
                }

                return filteredTickets.map {
                    GenericListItem(
                        it.name, null, null, selected = false
                    )
                }
            }

            return emptyList()
        }

        private fun formatTime(context: Context, timeInMillis: Long): String {
            val fromSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK)
            val toSdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.UK)
            // Use SystemTimeFormatManager singleton instead of requiring Context parameter
            val timePattern = SystemTimeFormatManager.getTimeFormatPatternWithAmPm()
            val toSdfTime = SimpleDateFormat(timePattern, Locale.UK)
            val fromDate = Date(timeInMillis)
            return "${toSdfDate.format(fromDate)} ${toSdfTime.format(fromDate)}"
        }

        private fun formatTimeRange(context: Context, startTimeInMillis: Long, endTimeInMillis: Long): String {
            val fromSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK)
            val toSdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.UK)
            // Use SystemTimeFormatManager singleton instead of requiring Context parameter
            val timePattern = SystemTimeFormatManager.getTimeFormatPatternWithAmPm()
            val toSdfTime = SimpleDateFormat(timePattern, Locale.UK)
            val startDate = Date(startTimeInMillis)
            val endDate = Date(endTimeInMillis)
            return "${toSdfDate.format(startDate)} ${toSdfTime.format(startDate)} - ${toSdfDate.format(endDate)} ${toSdfTime.format(endDate)}"
        }
    }
}