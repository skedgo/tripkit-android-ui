package com.skedgo.tripkit.analytics

enum class SearchResultItemSource(val value: String) {
    GoogleLocation("Google Maps Geocoding API"),
    Foursquare("Foursquare"),
    TripGo("TripGo"),
    Favorite("Favorite"),
    CurrentLocation("Current Location"),
    DropPin("Drop Pin"),
    Unknown("Unknown")
}
