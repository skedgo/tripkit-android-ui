package com.skedgo.tripkit.ui.utils

import com.skedgo.tripkit.ui.utils.DistanceFormatter.DistanceUnits.KILOMETERS
import com.skedgo.tripkit.ui.utils.DistanceFormatter.DistanceUnits.MILES
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatterTest {

    @Test
    fun `metric preference uses kilometers regardless of locale and region`() {
        assertEquals(
            KILOMETERS,
            DistanceFormatter.resolveDistanceUnit(Locale.UK, "metric", "GB_ENG_Leicester")
        )
    }

    @Test
    fun `imperial preference uses miles regardless of locale and region`() {
        assertEquals(
            MILES,
            DistanceFormatter.resolveDistanceUnit(
                Locale.forLanguageTag("en-AU"),
                "imperial",
                "AU_NT_Darwin"
            )
        )
    }

    @Test
    fun `auto preference uses metric trip region before imperial device locale`() {
        assertEquals(
            KILOMETERS,
            DistanceFormatter.resolveDistanceUnit(Locale.UK, "auto", "AU_NT_Darwin")
        )
    }

    @Test
    fun `auto preference uses imperial trip region before metric device locale`() {
        assertEquals(
            MILES,
            DistanceFormatter.resolveDistanceUnit(
                Locale.forLanguageTag("en-AU"),
                "auto",
                "GB_ENG_Leicester"
            )
        )
    }

    @Test
    fun `replaceDistanceInText updates compact API distance and preserves other notes`() {
        assertEquals(
            "Traffic ⋅ 15.5 mi ⋅ Includes toll ($4.00)",
            DistanceFormatter.replaceDistanceInText(
                "Traffic ⋅ 25km ⋅ Includes toll ($4.00)",
                "15.5 mi"
            )
        )
    }

    @Test
    fun `replaceDistanceInText updates spaced distance`() {
        assertEquals(
            "9.7 mi",
            DistanceFormatter.replaceDistanceInText("16 km", "9.7 mi")
        )
    }

    @Test
    fun `replaceDistanceInText does not treat minutes as metres`() {
        assertEquals(
            "Ride Bicycle for 51 mins",
            DistanceFormatter.replaceDistanceInText("Ride Bicycle for 51 mins", "9.7 mi")
        )
    }
}
