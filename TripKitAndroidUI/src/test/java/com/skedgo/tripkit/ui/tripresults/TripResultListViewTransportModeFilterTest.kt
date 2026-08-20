package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.routing.SimpleTransportModeFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TripResultListViewTransportModeFilterTest {

    private val viewFilter = TestTransportViewFilter()
    private val filter = TripResultListViewTransportModeFilter(
        SimpleTransportModeFilter(),
        viewFilter
    )

    @Test
    fun `car deselected and park ride deselected requests neither car nor a group`() {
        assertThat(filter.useTransportMode(TransportMode.ID_CAR)).isFalse()
        assertThat(filter.getModeRequestGroups()).isEmpty()
    }

    @Test
    fun `car deselected and park ride selected requests only the park ride group`() {
        viewFilter.setSelected(InjectedTransportModes.ID_PARK_RIDE, true)

        assertThat(filter.useTransportMode(TransportMode.ID_CAR)).isFalse()
        assertThat(filter.getModeRequestGroups().map { it.toSet() })
            .containsExactly(
                setOf(TransportMode.ID_CAR, TransportMode.ID_PUBLIC_TRANSPORT)
            )
        assertThat(viewFilter.isSelected(TransportMode.ID_CAR)).isFalse()
    }

    @Test
    fun `car selected and park ride deselected keeps normal car behavior`() {
        viewFilter.setSelected(TransportMode.ID_CAR, true)

        assertThat(filter.useTransportMode(TransportMode.ID_CAR)).isTrue()
        assertThat(filter.getModeRequestGroups()).isEmpty()
    }

    @Test
    fun `car and park ride selected keep normal car and park ride group`() {
        viewFilter.setSelected(TransportMode.ID_CAR, true)
        viewFilter.setSelected(InjectedTransportModes.ID_PARK_RIDE, true)

        assertThat(filter.useTransportMode(TransportMode.ID_CAR)).isTrue()
        assertThat(filter.getModeRequestGroups().map { it.toSet() })
            .containsExactly(
                setOf(TransportMode.ID_CAR, TransportMode.ID_PUBLIC_TRANSPORT)
            )
    }

    @Test
    fun `park ride custom identifier is never a normal backend mode`() {
        viewFilter.setSelected(InjectedTransportModes.ID_PARK_RIDE, true)

        assertThat(filter.useTransportMode(InjectedTransportModes.ID_PARK_RIDE)).isFalse()
    }

    @Test
    fun `deselecting park ride does not reselect car or retain its request group`() {
        viewFilter.setSelected(InjectedTransportModes.ID_PARK_RIDE, true)
        viewFilter.setSelected(InjectedTransportModes.ID_PARK_RIDE, false)

        assertThat(viewFilter.isSelected(TransportMode.ID_CAR)).isFalse()
        assertThat(filter.useTransportMode(TransportMode.ID_CAR)).isFalse()
        assertThat(filter.getModeRequestGroups()).isEmpty()
    }

    private class TestTransportViewFilter : TripResultTransportViewFilter {
        private val selectedModes = mutableSetOf<String>()
        private val minimizedModes = mutableSetOf<String>()

        override fun isSelected(mode: String): Boolean = mode in selectedModes

        override fun isMinimized(mode: String): Boolean = mode in minimizedModes

        override fun setSelected(mode: String, selected: Boolean) {
            if (selected) {
                selectedModes.add(mode)
            } else {
                selectedModes.remove(mode)
            }
        }

        override fun setMinimized(mode: String, minimized: Boolean) {
            if (minimized) {
                minimizedModes.add(mode)
            } else {
                minimizedModes.remove(mode)
            }
        }
    }
}
