package com.skedgo.tripkit.ui.timetables

import android.text.Html
import com.skedgo.tripkit.ui.model.TimetableEntry
import javax.inject.Inject

private const val MID_DOT = " &middot; "

open class GetDirectionText @Inject constructor() {

  open fun execute(service: TimetableEntry): String {
    val direction = when {
          !service.serviceDirection.isNullOrEmpty() -> service.serviceDirection!!
          !service.startStop?.name.isNullOrEmpty() -> service.startStop.name!!
          else -> ""
        }
    return when {
      service.startStop?.shortName?.isNotEmpty() == true ->
        Html.fromHtml(service.startStop.shortName + MID_DOT + direction).toString()
      else -> direction
    }
  }
}