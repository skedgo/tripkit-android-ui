package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.view.accessibility.AccessibilityManager
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccessibilityDefaultViewManagerTest {
    @Test
    fun `repeated view attachment registers only one accessibility observer`() {
        val context = mockk<Context>()
        val manager = mockk<AccessibilityManager>(relaxed = true)
        every { context.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns manager
        val owner = AccessibilityDefaultViewManager(context)
        owner.setAccessibilityObserver()
        owner.setAccessibilityObserver()
        verify(exactly = 1) { manager.addAccessibilityStateChangeListener(any()) }
    }
    @Test
    fun `destroyed view removes its exact observer and can attach again`() {
        val context = mockk<Context>()
        val manager = mockk<AccessibilityManager>(relaxed = true)
        every { context.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns manager
        val observer = slot<AccessibilityManager.AccessibilityStateChangeListener>()
        every { manager.addAccessibilityStateChangeListener(capture(observer)) } returns true
        val owner = AccessibilityDefaultViewManager(context)
        owner.setAccessibilityObserver()
        owner.removeAccessibilityObserver()
        owner.removeAccessibilityObserver()
        verify(exactly = 1) { manager.removeAccessibilityStateChangeListener(observer.captured) }
        owner.setAccessibilityObserver()
        verify(exactly = 2) { manager.addAccessibilityStateChangeListener(any()) }
    }

}
