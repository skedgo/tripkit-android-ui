package com.skedgo.tripkit.ui

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Redmine #19351: landscape variants of the timetable and trip-details layouts place the header
 * in a scrollable start column. They must inflate (an AndroidX NestedScrollView with
 * android:nestedScrollingEnabled in XML crashes during inflation) and keep the ids the
 * fragments bind to.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "land")
class LandscapeLayoutInflationTest {

    private fun inflate(layout: Int): View {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar
        )
        return LayoutInflater.from(context).inflate(layout, null)
    }

    @Test
    fun `landscape timetable layout inflates with list and go-to-now button`() {
        val root = inflate(R.layout.timetable_fragment)

        assertNotNull(root.findViewById(R.id.landscapeSplit))
        assertNotNull(root.findViewById(R.id.recyclerView))
        assertNotNull(root.findViewById(R.id.goToNowButton))
        assertNotNull(root.findViewById(R.id.departuresSearchSetTime))
    }

    @Test
    fun `landscape trip segment list layout inflates with segment list`() {
        val root = inflate(R.layout.trip_segment_list_fragment)

        assertNotNull(root.findViewById(R.id.landscapeSplit))
        assertNotNull(root.findViewById(R.id.itemsView))
        assertNotNull(root.findViewById(R.id.buttonLayout))
    }
}
