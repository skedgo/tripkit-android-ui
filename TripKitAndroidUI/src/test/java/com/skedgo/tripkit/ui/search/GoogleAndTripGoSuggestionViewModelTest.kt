package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.Place.TripGoPOI
import com.squareup.picasso.Picasso
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoogleAndTripGoSuggestionViewModelTest {

    private lateinit var context: Context
    private lateinit var picasso: Picasso
    private lateinit var place: Place
    private lateinit var iconProvider: LocationSearchIconProvider
    private lateinit var viewModel: GoogleAndTripGoSuggestionViewModel

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        picasso = mockk(relaxed = true)
        place = mockk(relaxed = true)
        iconProvider = mockk(relaxed = true)

        every { context.getString(R.string.unknown_location) } returns "Unknown Location"
        every { ContextCompat.getDrawable(context, any()) } returns mockk<Drawable>()

        viewModel = GoogleAndTripGoSuggestionViewModel(
            context = context,
            picasso = picasso,
            place = place,
            canOpenTimetable = false,
            iconProvider = iconProvider,
            query = "Test Query"
        )
    }

    @Test
    fun `retained place suggestions refresh title and subtitle colors across theme changes`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val retained = GoogleAndTripGoSuggestionViewModel(
            app, picasso, TripGoPOI(Location(42.33, -83.04).apply {
                name = "Detroit destination"
                address = "Detroit, Michigan"
            }), false, iconProvider, "Detroit"
        )
        assertRetainedSuggestionContrast(retained)
    }

    @Test
    fun `retained choose on map suggestion refreshes colors when search is reopened in another theme`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val retained = FixedSuggestionViewModel(app, DefaultSearchSuggestion(
            FixedSuggestions.CHOOSE_ON_MAP, "Choose on Map", "Location information",
            R.color.title_text, R.color.description_text,
            android.graphics.drawable.ColorDrawable(android.graphics.Color.GRAY)
        ))
        assertRetainedSuggestionContrast(retained)
    }

    private fun assertRetainedSuggestionContrast(retained: SuggestionViewModel) {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        try {
            for (qualifiers in listOf("night", "notnight", "night", "notnight")) {
                org.robolectric.RuntimeEnvironment.setQualifiers(qualifiers)
                val row = com.skedgo.tripkit.ui.search.compose.mapSearchResultRows(listOf(retained)).single()
                val surface = app.getColor(R.color.subCardBackground)
                assertTrue("Title must remain readable in $qualifiers",
                    androidx.core.graphics.ColorUtils.calculateContrast(row.titleTextColor, surface) >= 4.5)
                assertTrue("Subtitle must remain readable in $qualifiers",
                    androidx.core.graphics.ColorUtils.calculateContrast(row.subtitleTextColor, surface) >= 3.0)
            }
        } finally {
            org.robolectric.RuntimeEnvironment.setQualifiers("")
        }
    }

    @Test
    fun `title should return location name if available`() {
        val location: Location = mockk(relaxed = true) {
            every { name } returns "Test Place"
            every { address } returns "123 Main St"
        }

        val tripGoPOI = TripGoPOI(location)

        val viewModel = GoogleAndTripGoSuggestionViewModel(
            context = context,
            picasso = picasso,
            place = tripGoPOI,
            canOpenTimetable = false,
            iconProvider = iconProvider,
            query = "Test Query"
        )

        assertEquals("Test Place", viewModel.title)
    }

    @Test
    fun `title should return location address if name is null`() {
        val location: Location = mockk(relaxed = true) {
            every { name } returns null
            every { address } returns "123 Main St"
        }

        val tripGoPOI = TripGoPOI(location)

        val viewModel = GoogleAndTripGoSuggestionViewModel(
            context = context,
            picasso = picasso,
            place = tripGoPOI,
            canOpenTimetable = false,
            iconProvider = iconProvider,
            query = "Test Query"
        )

        assertEquals("123 Main St", viewModel.title)
    }

    @Test
    fun `title should return unknown location if both name and address are null`() {
        val location: Location = mockk(relaxed = true) {
            every { name } returns null
            every { address } returns null
        }
        val tripGoPOI = TripGoPOI(location)

        val viewModel = GoogleAndTripGoSuggestionViewModel(
            context = context,
            picasso = picasso,
            place = tripGoPOI,
            canOpenTimetable = false,
            iconProvider = iconProvider,
            query = "Test Query"
        )

        assertEquals("Unknown Location", viewModel.title)
    }

    @Test
    fun `icon should be set based on location type`() {
        every {
            iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.HOME)
        } returns R.drawable.home

        assertNotNull(viewModel.icon.get())
    }

    @Test
    fun `should initialize tap actions`() {
        assertNotNull(viewModel.onItemClicked)
        assertNotNull(viewModel.onInfoClicked)
        assertNotNull(viewModel.onSuggestionActionClicked)
    }

    @Test
    fun `showTimetableIcon should be true for scheduled stops`() {
        val scheduledStop: ScheduledStop = mockk(relaxed = true)
        val tripGoPOI = TripGoPOI(scheduledStop)

        val testViewModel = GoogleAndTripGoSuggestionViewModel(
            context = context,
            picasso = picasso,
            place = tripGoPOI,
            canOpenTimetable = true,
            iconProvider = iconProvider,
            query = "Test Query",
        )

        assertTrue(testViewModel.showTimetableIcon)
    }
}