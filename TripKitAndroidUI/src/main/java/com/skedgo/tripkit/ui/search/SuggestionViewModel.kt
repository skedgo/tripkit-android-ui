package com.skedgo.tripkit.ui.search

import android.content.Context
    import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.utils.TapAction
import com.squareup.picasso.Picasso

sealed class SuggestionViewModel(
    protected val context: Context,
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

    /**
     * Applies tint programmatically to icons for dark mode support.
     * 
     * This approach is used because drawables are not uniform in format (some are PNGs which can't
     * be easily themed with night-res) and not all can be supported with night-res theming. Some
     * icons (like HOME and WORK) have circular backgrounds that would break if tinted, so we skip
     * tinting for those. We'll audit and request proper night mode variants in the next dark mode
     * support iteration.
     */
    protected fun applyIconTintIfNeeded(drawable: Drawable?, id: Any?, locationType: Int?): Drawable? {
        if (drawable == null) return null
        
        // Skip tinting for HOME and WORK icons (they have circular backgrounds that would break with tinting)
        // Check ID first (for FixedSuggestions from either package), then fall back to location type
        val isHomeOrWork = if (id is Enum<*>) {
            val enumName = id.name
            enumName == "HOME" || enumName == "WORK"
        } else {
            false
        } || locationType == Location.TYPE_HOME || locationType == Location.TYPE_WORK
        
        if (isHomeOrWork) {
            return drawable
        }
        
        // Apply tint for all other icons
        val tintColor = ContextCompat.getColor(context, R.color.icon_tint_default)
        drawable.mutate().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        return drawable
    }
}

open class FixedSuggestionViewModel(
    context: Context,
    suggestion: SearchSuggestion,
    term: String? = null
) :
    SuggestionViewModel(context, term) {
    val suggestion = suggestion
    val id = suggestion.id()
    override val title = suggestion.title()
    override val titleTextColorRes = suggestion.titleColor()
    override val subtitle = suggestion.subtitle()
    override val subtitleTextColorRes = suggestion.subtitleColor()
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val showInfoIcon =
        suggestion.location() != null && suggestion.location() !is ScheduledStop
    override val onInfoClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val onSuggestionActionClicked: TapAction<SuggestionViewModel> =
        TapAction.create { this }

    init {
        val originalIcon = suggestion.icon()
        val suggestionId = suggestion.id()
        val locationType = suggestion.location()?.locationType
        icon.set(applyIconTintIfNeeded(originalIcon, suggestionId, locationType))
    }
}

class SearchProviderSuggestionViewModel(
    context: Context,
    suggestion: SearchSuggestion,
    term: String? = null
) :
    FixedSuggestionViewModel(context, suggestion, term)

class HistorySearchProviderSuggestionViewModel(
    context: Context,
    suggestion: SearchSuggestion,
    term: String? = null
) : SuggestionViewModel(context, term) {
    val suggestion = suggestion
    val id = suggestion.id()
    override val title = suggestion.title()
    override val titleTextColorRes = suggestion.titleColor()
    override val subtitle = suggestion.subtitle()
    override val subtitleTextColorRes = suggestion.subtitleColor()
    override val onItemClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val showInfoIcon =
        suggestion.location() != null && suggestion.location() !is ScheduledStop
    override val showTimetableIcon: Boolean
        get() = suggestion.location() is ScheduledStop
    override val onInfoClicked: TapAction<SuggestionViewModel> = TapAction.create { this }
    override val onSuggestionActionClicked: TapAction<SuggestionViewModel> =
        TapAction.create { this }

    init {
        icon.set(suggestion.icon())
    }
}

class CityProviderSuggestionViewModel(
    context: Context,
    suggestion: SearchSuggestion,
    term: String? = null
) :
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
    override val onSuggestionActionClicked: TapAction<SuggestionViewModel> =
        TapAction.create { this }

    val location: Location by lazy {
        if (place is Place.TripGoPOI) {
            place.location
        } else {
            val prediction = (place as Place.WithoutLocation).prediction
            val location =
                Location(-1.0, -1.0)
            location.source = place.source()
            location.locationType = place.locationType()
            location.name = prediction.primaryText
            location.address = prediction.secondaryText
            location
        }
    }

    override val title: String by lazy {

        if (!location.name.isNullOrEmpty()) {
            return@lazy location.name!!
        }

        if (!location.address.isNullOrEmpty()) {
            return@lazy location.address!!
        }

        return@lazy context.getString(R.string.unknown_location)
    }

    override val subtitle: String? by lazy {
        var subtitle = location.address

        if (place is Place.TripGoPOI && location is ScheduledStop) {
            val scheduledStop = location as ScheduledStop
            if (!scheduledStop.services.isNullOrEmpty()) {
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
                val placeIcon = place.icon()
                icon.set(applyIconTintIfNeeded(placeIcon, null, location.locationType))
            }
            iconRes == 0 -> {
                icon.set(null)
            }
            else -> {
                val drawable = ContextCompat.getDrawable(context, iconRes)
                icon.set(applyIconTintIfNeeded(drawable, null, location.locationType))
            }
        }
    }
}