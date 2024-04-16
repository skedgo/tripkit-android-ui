package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.utils.TapAction
import com.squareup.picasso.Picasso

sealed class SuggestionViewModel(
    context: Context,
    val term: String? = null
) {
    val icon: ObservableField<Drawable?> = ObservableField()
    open val showTimetableIcon: Boolean = false
    open val showInfoIcon: Boolean = false
    abstract val title: String
    open val subtitle: String? = null

    val titleTextColor: Int by lazy { ContextCompat.getColor(context, titleTextColorRes) }
    val subtitleTextColor: Int by lazy { ContextCompat.getColor(context, subtitleTextColorRes) }

    protected abstract val titleTextColorRes: Int
    protected abstract val subtitleTextColorRes: Int

    abstract val onItemClicked: TapAction<SuggestionViewModel>

    abstract val onInfoClicked: TapAction<SuggestionViewModel>

    abstract val onSuggestionActionClicked: TapAction<SuggestionViewModel>
}

open class FixedSuggestionViewModel(context: Context, suggestion: SearchSuggestion, term: String? = null) :
    SuggestionViewModel(context, term) {
    val suggestion = suggestion
    val id = suggestion.id()
    override val title = suggestion.title()
    override val titleTextColorRes = suggestion.titleColor()
    override val subtitle = suggestion.subtitle()
    override val subtitleTextColorRes = suggestion.subtitleColor()
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val showInfoIcon = suggestion.location() != null && suggestion.location() !is ScheduledStop
    override val onInfoClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val onSuggestionActionClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    init {
        icon.set(suggestion.icon())
    }
}

class SearchProviderSuggestionViewModel(context: Context, suggestion: SearchSuggestion, term: String? = null) :
    FixedSuggestionViewModel(context, suggestion, term)

class CityProviderSuggestionViewModel(context: Context, suggestion: SearchSuggestion, term: String? = null) :
    FixedSuggestionViewModel(context, suggestion, term)

class GoogleAndTripGoSuggestionViewModel(
    context: Context,
    val picasso: Picasso,
    val place: Place,
    val canOpenTimetable: Boolean,
    val iconProvider: LocationSearchIconProvider,
    val query: String?
) : SuggestionViewModel(context, query) {

    override val titleTextColorRes: Int = R.color.title_text
    override val subtitleTextColorRes: Int = R.color.description_text
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val onInfoClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val onSuggestionActionClicked: TapAction<SuggestionViewModel> = TapAction.create { this }

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

    override val showInfoIcon = location !is ScheduledStop

    init {
        val iconRes = if (location is ScheduledStop) {
            iconProvider.iconForSearchResult(
                LocationSearchIconProvider.SearchResultType.SCHEDULED_STOP,
                (location as ScheduledStop).type
            )
        } else {
            when (location.locationType) {
                Location.TYPE_CONTACT -> {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CONTACT)
                }
                Location.TYPE_CALENDAR -> {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CALENDAR)
                }
                Location.TYPE_W3W -> {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.W3W)
                }
                Location.TYPE_HOME -> {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.HOME)
                }
                Location.TYPE_WORK -> {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.WORK)
                }
                Location.TYPE_HISTORY -> {
                    val default =
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.HISTORY)
                    if (default <= 0) iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.GOOGLE)
                    else default
                }
                Location.TYPE_SCHOOL -> {
                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.SCHOOL)
                }
                else -> {
                    val source = location.source
                    when {
                        Location.FOURSQUARE == source -> {
                            iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.FOURSQUARE)
                        }
                        Location.GOOGLE == source -> {
                            iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.GOOGLE)
                        }
                        else -> {
                            iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.GOOGLE)
                        }
                    }
                }
            }
        }

        when {
            place.icon() != null -> {
                icon.set(place.icon())
            }
            iconRes == 0 -> {
                icon.set(null)
            }
            else -> {
                icon.set(ContextCompat.getDrawable(context, iconRes))
            }
        }
    }
}