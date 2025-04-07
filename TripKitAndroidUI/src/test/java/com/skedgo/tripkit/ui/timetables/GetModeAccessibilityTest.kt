package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.common.model.BicycleAccessible
import com.skedgo.tripkit.common.model.WheelchairAccessible
import com.skedgo.tripkit.ui.timetables.GetModeAccessibility
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetModeAccessibilityTest {

    private lateinit var getModeAccessibility: GetModeAccessibility

    @Before
    fun setUp() {
        getModeAccessibility = GetModeAccessibility()
    }

    @Test
    fun `should return 1 when wheelchair is accessible`() {
        val wheelchairAccessible = mockk<WheelchairAccessible> {
            every { wheelchairAccessible } returns true
        }

        val result = getModeAccessibility.wheelchair(wheelchairAccessible)
        assertEquals(1, result)
    }

    @Test
    fun `should return 0 when wheelchair is not accessible`() {
        val wheelchairAccessible = mockk<WheelchairAccessible> {
            every { wheelchairAccessible } returns false
        }

        val result = getModeAccessibility.wheelchair(wheelchairAccessible)
        assertEquals(0, result)
    }

    @Test
    fun `should return -1 when wheelchair accessibility is unknown`() {
        val wheelchairAccessible = mockk<WheelchairAccessible> {
            every { wheelchairAccessible } returns null
        }

        val result = getModeAccessibility.wheelchair(wheelchairAccessible)
        assertEquals(-1, result)
    }

    @Test
    fun `should return 1 when bicycle is accessible`() {
        val bicycleAccessible = mockk<BicycleAccessible> {
            every { bicycleAccessible } returns true
        }

        val result = getModeAccessibility.bicycle(bicycleAccessible)
        assertEquals(1, result)
    }

    @Test
    fun `should return 0 when bicycle is not accessible`() {
        val bicycleAccessible = mockk<BicycleAccessible> {
            every { bicycleAccessible } returns false
        }

        val result = getModeAccessibility.bicycle(bicycleAccessible)
        assertEquals(0, result)
    }

    @Test
    fun `should return -1 when bicycle accessibility is unknown`() {
        val bicycleAccessible = mockk<BicycleAccessible> {
            every { bicycleAccessible } returns null
        }

        val result = getModeAccessibility.bicycle(bicycleAccessible)
        assertEquals(-1, result)
    }
}
