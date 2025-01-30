package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IconUtilsTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var resources: Resources

    @Before
    fun setup() {
        // Use a real instance and spy it
        resources = mockk<Resources>(relaxed = true)
    }

    @Test
    fun `asUrl should format URL correctly for mdpi`() {
        every { resources.displayMetrics } returns DisplayMetrics().apply {
            densityDpi = DisplayMetrics.DENSITY_MEDIUM
        }

        val icon = "test_icon"
        val urlTemplate = "https://example.com/icons/%s/%s.png"
        val expectedUrl = "https://example.com/icons/mdpi/$icon.png"

        val result = IconUtils.asUrl(resources, icon, urlTemplate)
        assertEquals(expectedUrl, result)
    }

    @Test
    fun `asUrl should format URL correctly for hdpi`() {
        every { resources.displayMetrics } returns DisplayMetrics().apply {
            densityDpi = DisplayMetrics.DENSITY_HIGH
        }

        val icon = "test_icon"
        val urlTemplate = "https://example.com/icons/%s/%s.png"
        val expectedUrl = "https://example.com/icons/hdpi/$icon.png"

        val result = IconUtils.asUrl(resources, icon, urlTemplate)

        assertEquals(expectedUrl, result)
    }

    @Test
    fun `asUrl should format URL correctly for xhdpi`() {
        every { resources.displayMetrics } returns DisplayMetrics().apply {
            densityDpi = DisplayMetrics.DENSITY_XHIGH
        }

        val icon = "test_icon"
        val urlTemplate = "https://example.com/icons/%s/%s.png"
        val expectedUrl = "https://example.com/icons/xhdpi/$icon.png"

        val result = IconUtils.asUrl(resources, icon, urlTemplate)

        assertEquals(expectedUrl, result)
    }

    @Test
    fun `asUrl should format URL correctly for xxhdpi`() {
        every { resources.displayMetrics } returns DisplayMetrics().apply {
            densityDpi = DisplayMetrics.DENSITY_XXHIGH
        }

        val icon = "test_icon"
        val urlTemplate = "https://example.com/icons/%s/%s.png"
        val expectedUrl = "https://example.com/icons/xxhdpi/$icon.png"

        val result = IconUtils.asUrl(resources, icon, urlTemplate)

        assertEquals(expectedUrl, result)
    }

    @Test
    fun `asUrl should format URL correctly for unknown density (defaults to xxxhdpi)`() {
        every { resources.displayMetrics } returns DisplayMetrics().apply {
            densityDpi = 999
        }

        val icon = "test_icon"
        val urlTemplate = "https://example.com/icons/%s/%s.png"
        val expectedUrl = "https://example.com/icons/xxxhdpi/$icon.png"

        val result = IconUtils.asUrl(resources, icon, urlTemplate)

        assertEquals(expectedUrl, result)
    }
}