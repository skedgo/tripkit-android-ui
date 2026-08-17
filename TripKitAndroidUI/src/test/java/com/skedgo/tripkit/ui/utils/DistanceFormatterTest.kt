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
}
