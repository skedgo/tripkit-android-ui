package com.skedgo.tripkit.ui.booking

import android.text.TextUtils
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.Fragment
import com.skedgo.tripkit.bookingproviders.BookingResolver
import com.skedgo.tripkit.routing.TripSegment

class BookViewClickEventHandler private constructor(private val fragment: Fragment) {
    private fun performAction(
        bookingResolver: BookingResolver,
        action: String,
        segment: TripSegment
    ) {
    }

    private fun showActionsDialog(bookingResolver: BookingResolver, segment: TripSegment) {
        val externalActions = segment.booking!!.externalActions
        if (externalActions != null) {
            val titles = getActionTitles(externalActions, bookingResolver)
            Builder(fragment.requireContext())
                .setItems(titles) { dialog, which ->
                    performAction(
                        bookingResolver,
                        externalActions[which],
                        segment
                    )
                }
                .create()
                .show()
        }
    }

    companion object {
        @JvmStatic
        fun create(fragment: Fragment): BookViewClickEventHandler {
            return BookViewClickEventHandler(fragment)
        }

        fun getActionTitles(
            externalActions: List<String?>,
            bookingResolver: BookingResolver
        ): Array<String?> {
            val titles = arrayOfNulls<String>(externalActions.size)
            for (i in externalActions.indices) {
                val title = bookingResolver.getTitleForExternalAction(externalActions[i]!!)
                titles[i] = if (TextUtils.isEmpty(title)) externalActions[i] else title
            }

            return titles
        }
    }
}