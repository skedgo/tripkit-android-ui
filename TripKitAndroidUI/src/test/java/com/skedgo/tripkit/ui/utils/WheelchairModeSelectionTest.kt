package com.skedgo.tripkit.ui.utils

import android.content.SharedPreferences
import com.skedgo.tripkit.common.model.TransportMode
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

/**
 * The wheelchair mode has no preference key of its own — it is selected exactly when walking is
 * not. `PrefsBasedTransportViewFilter` and `GetRoutingConfigImpl` both read the flag through
 * [isWheelchairModeSelected], so this asserts the one rule they share.
 */
class WheelchairModeSelectionTest {

    private val prefs: SharedPreferences = mockk()

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `walking selected means wheelchair is not selected`() {
        every { prefs.getBoolean(TransportMode.ID_WALK, true) } returns true

        assertThat(prefs.isWheelchairModeSelected()).isFalse()
    }

    @Test
    fun `walking deselected means wheelchair is selected`() {
        every { prefs.getBoolean(TransportMode.ID_WALK, true) } returns false

        assertThat(prefs.isWheelchairModeSelected()).isTrue()
    }

    @Test
    fun `walking defaults to selected so wheelchair defaults to unselected`() {
        every { prefs.getBoolean(TransportMode.ID_WALK, true) } answers { secondArg() }

        assertThat(prefs.isWheelchairModeSelected()).isFalse()
    }
}
