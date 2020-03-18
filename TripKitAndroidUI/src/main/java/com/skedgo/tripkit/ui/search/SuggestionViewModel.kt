package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.utils.BindingConversions
import com.skedgo.tripkit.ui.utils.StopMarkerUtils
import com.skedgo.tripkit.ui.utils.TapAction
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import timber.log.Timber

sealed class SuggestionViewModel(context: Context) {
    val icon: ObservableField<Drawable?> = ObservableField()
    open val showTimetableIcon: Boolean = false
    abstract val title: String
    open val subtitle: String? = null

    val titleTextColor: Int by lazy { ContextCompat.getColor(context, titleTextColorRes) }
    val subtitleTextColor: Int by lazy { ContextCompat.getColor(context, subtitleTextColorRes) }

    protected abstract val titleTextColorRes: Int
    protected abstract val subtitleTextColorRes: Int

    abstract val onItemClicked: TapAction<SuggestionViewModel>
}

class CurrentLocationSuggestionViewModel(context: Context) : SuggestionViewModel(context) {
    override val title = context.getString(R.string.current_location)
    override val titleTextColorRes: Int = R.color.main_color
    override val subtitleTextColorRes: Int = R.color.main_color
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }

    init {
        icon.set(ContextCompat.getDrawable(context, R.drawable.ic_currentlocation))
    }
}

class DropNewPinSuggestionViewModel(context: Context) : SuggestionViewModel(context) {
    override val title = context.getString(R.string.drop_new_pin)
    override val titleTextColorRes: Int = R.color.main_color
    override val subtitleTextColorRes: Int = R.color.main_color
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }

    init {
        icon.set(ContextCompat.getDrawable(context, R.drawable.ic_chooseonmap))
    }
}

class GoogleAndTripGoSuggestionViewModel(context: Context,
                                         val picasso: Picasso,
                                         val place: Place,
                                         val canOpenTimetable: Boolean,
                                         val iconProvider: LocationSearchIconProvider,
                                         val query: String?) : SuggestionViewModel(context) {
    override val titleTextColorRes: Int = R.color.title_text
    override val subtitleTextColorRes: Int = R.color.description_text
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }

    val location: Location by lazy {
        if (place is Place.TripGoPOI) {
            place.location
        } else {
            val prediction = (place as Place.WithoutLocation).prediction
            val location = Location(-1.0, -1.0)
            location.source = place.source()
            location.locationType = place.locationType()
            location.name = prediction.primaryText
            location.address = prediction.secondaryText
            location
        }
    }

    override val title: String by lazy {
        if (!location.name.isNullOrEmpty()) {
            return@lazy location.name
        }

        if (!location.address.isNullOrEmpty()) {
            return@lazy location.address
        }

        return@lazy context.getString(R.string.unknown_location)
    }

    override val subtitle: String? by lazy {
        var subtitle = location.address

        if (place is Place.TripGoPOI && location is ScheduledStop) {
            val scheduledStop = location as ScheduledStop
            if (!scheduledStop.services.isEmpty()) {
                subtitle = scheduledStop.services
            }

            if (query == scheduledStop.code) {
                subtitle = subtitle + " - " + scheduledStop.code
            }
        }

        if (location.name != subtitle) {
            return@lazy subtitle
        } else {
            return@lazy null
        }
    }

    override val showTimetableIcon = canOpenTimetable && location is ScheduledStop

    init {
        val iconRes = if (location is ScheduledStop) {
            iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.SCHEDULED_STOP, (location as ScheduledStop).type)
        } else {
            if (location.locationType == Location.TYPE_CONTACT) {
                iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CONTACT)
            } else if (location.locationType == Location.TYPE_CALENDAR) {
                iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CALENDAR)
            } else if (location.locationType == Location.TYPE_W3W) {
                iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.W3W)
            } else if (location.locationType == Location.TYPE_HOME) {
                iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.HOME)
            } else if (location.locationType == Location.TYPE_WORK) {
                iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.WORK)
            } else {
                val source = location.source
                if (Location.FOURSQUARE == source) {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.FOURSQUARE)
                } else if (Location.GOOGLE == source) {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.GOOGLE)
                } else {
                    0
                }
            }
        }

        if (place.icon() != null) {
            icon.set(place.icon())
        } else if (iconRes == 0) {
            icon.set(null)
        } else {
            icon.set(ContextCompat.getDrawable(context, iconRes))
        }
    }
}