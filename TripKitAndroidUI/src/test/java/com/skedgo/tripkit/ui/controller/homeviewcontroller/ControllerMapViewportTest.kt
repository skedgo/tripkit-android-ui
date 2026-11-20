package com.skedgo.tripkit.ui.controller.homeviewcontroller

import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerMapViewportTest {
    @Test fun `padding follows the measured stable sheet position`() {
        assertEquals(400, controllerMapBottomPadding(1000, 600))
        assertEquals(0, controllerMapBottomPadding(1000, 1000))
    }
    @Test fun `expanded sheet and offscreen positions cannot create invalid map bounds`() {
        assertEquals(999, controllerMapBottomPadding(1000, -200))
        assertEquals(0, controllerMapBottomPadding(1000, 1200))
        assertEquals(0, controllerMapBottomPadding(0, 0))
    }
}
