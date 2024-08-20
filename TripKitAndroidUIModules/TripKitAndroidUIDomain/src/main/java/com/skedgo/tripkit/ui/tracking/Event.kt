package com.skedgo.tripkit.ui.tracking

import com.skedgo.tripkit.analytics.SearchResultItemSource

sealed class Event(val name: String) {
    class ViewPriorities : Event("View_Priorities")
    class AgendaSetupCompletion : Event("Agenda_Setup_Completed")
    data class ViewCreditSources(val sources: String) : Event("View_Credit_Sources")
    data class StreetView(val lat: Double, val lng: Double) : Event("Street_View")
    class RateViaAskNicely : Event("Rate")
    class DislikeRating : Event("Dislike_Rating")
    class FeedbackViaAskNicely : Event("Feedback")
    data class ViewTrip(val source: String) : Event("View_Trip")
    data class FinishOnboarding(
        val didChangeModes: Boolean,
        val didChangePriorities: Boolean,
        val hasHome: Boolean = false,
        val hasWork: Boolean = false
    ) : Event("Finish_Onboarding")

    data class SelectSearchResultItem(
        val source: SearchResultItemSource,
        val position: Long
    ) : Event("Select_Search_Result_Item")

    /**
     * See https://developer.android.com/guide/components/intents-common.html#Maps.
     */
    data class LaunchedViaGeoIntents(
        val uri: String
    ) : Event("Launched_via_Geo_Intents")


    sealed class PushNotificationEvent(name: String) : Event(name) {
        object AppOpenedEvent : PushNotificationEvent("AppOpenedEvent")
        object AccountSetUpEvent : PushNotificationEvent("AccountSetUpEvent")
        object JourneyPlannerUsedEvent : PushNotificationEvent("JourneyPlannerUsedEvent")
        object ParkingUsedEvent : PushNotificationEvent("ParkingUsedEvent")
        object MykiUsedEvent : PushNotificationEvent("MykiUsedEvent")
    }

    class UberRideBooked(val cost: String, val currency: String, val uberService: String) :
        Event("Uber_Ride_booked")

    class PromoCodeCopied(val promoCode: String) : Event("promo_code_copied")
    object RoutesCalculated : Event("routes_calculated")
    object ParkingTabSelected : Event("parking_tab_selected")
    object MykiCardsTabSelected : Event("myki_cards_tab_selected")
    object FAQs : Event("faq_accessed")
    object ServiceAlerts : Event("service_disruptions_accessed")
    object TripsPlanned : Event("trip_planned")
    object FavouriteTripPlanned : Event("favourite_trip_planned")
    object FavouriteDestinationRouteCalculated : Event("favourite_destination_routes_calculated")
    object FavouriteTripRouteCalculated : Event("favourite_trip_routes_calculated")
    object MykiSignIn : Event("Signed_in_myki")
    object ParkingAddedToFavourites : Event("parking_add_to_favourite")
    object ParkingRemovedToFavourites : Event("parking_remove_from_favourite")
    class TransportIconSelected(val iconType: String?) : Event("map_icon_selected")

    class TopUpButtonPressed(
        val type: String,
        val price: Double,
        val zone: String? = null,
        val passDays: String? = null
    ) : Event("Top_Up_button_pressed")

    class MykiTopupMoney(val amount: String, val currency: String?) :
        Event("Successful_Myki_Topup_Money")

    class MykiTopupPass(
        val passDays: String,
        val currency: String?,
        val price: String,
        val zone: String
    ) : Event("Successful_Myki_Topup_Pass")
}