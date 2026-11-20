package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller.TKUITripDetailsViewControllerFragment
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.tripresult.TripResultPagerFragment
import com.skedgo.tripkit.ui.tripresult.TripResultMapContributor
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DetailsMapAttachmentTest {
    @Test fun `map arriving before pager does not clear its contributor`() {
        val map = mockk<TripKitMapFragment>(relaxed = true)
        TKUITripDetailsViewControllerFragment().tripKitMapFragment = map
        verify(exactly = 0) { map.setContributor(null) }
    }

    @Test fun `pager ready before map attaches the actual trip contributor`() {
        val controller = TKUITripDetailsViewControllerFragment()
        val contributor = mockk<TripResultMapContributor>(relaxed = true)
        val pager = mockk<TripResultPagerFragment>(relaxed = true)
        every { pager.contributor() } returns contributor
        every { pager.isAdded } returns true
        controller.javaClass.getDeclaredField("pagerFragment").apply { isAccessible = true }.set(controller, pager)
        val map = mockk<TripKitMapFragment>(relaxed = true)
        controller.tripKitMapFragment = map
        verify { map.setContributor(contributor) }
    }
    @Test fun `sheet card relayout uses final behavior position for map padding`() {
        val context = android.view.ContextThemeWrapper(androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        val parent = androidx.coordinatorlayout.widget.CoordinatorLayout(context)
        val sheetView = android.widget.FrameLayout(context)
        parent.addView(sheetView, androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(-1, -1).apply {
            behavior = com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout>()
        })
        val binding = mockk<com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeViewControllerBinding>(relaxed = true)
        for ((field, value) in listOf("mainLayout" to parent, "standardBottomSheet" to sheetView))
            com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeViewControllerBinding::class.java.getField(field).apply { isAccessible = true }.set(binding, value)
        val home = com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment()
        com.skedgo.tripkit.ui.core.BaseFragment::class.java.getDeclaredField("binding").apply { isAccessible = true }.set(home, binding)
        val map = mockk<com.google.android.gms.maps.GoogleMap>(relaxed = true)
        home.javaClass.getDeclaredField("map").apply { isAccessible = true }.set(home, map)
        val sheet = binding.standardBottomSheet
        sheet.visibility = android.view.View.VISIBLE
        val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
        behavior.isFitToContents = false
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
        home.observeMapViewport()
        val spec = android.view.View.MeasureSpec.EXACTLY
        binding.mainLayout.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, spec),
            android.view.View.MeasureSpec.makeMeasureSpec(2274, spec))
        binding.mainLayout.layout(0, 0, 1080, 2274)
        binding.mainLayout.viewTreeObserver.dispatchOnGlobalLayout()
        org.junit.Assert.assertEquals(1137, sheet.top)
        verify { map.setPadding(0, 0, 0, 1137) }
    }

    @Test fun `large preview header leaves room for segment markers in landscape`() {
        val context = android.view.ContextThemeWrapper(androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        val parent = androidx.coordinatorlayout.widget.CoordinatorLayout(context)
        val header = android.widget.FrameLayout(context)
        parent.addView(header, androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(-1, 328))
        val sheet = android.widget.FrameLayout(context)
        val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout>().apply {
            isFitToContents = false
            peekHeight = 315
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
        }
        parent.addView(sheet, androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(-1, -1).apply { this.behavior = behavior })
        val binding = mockk<com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeViewControllerBinding>(relaxed = true)
        for ((field, value) in listOf("mainLayout" to parent, "standardBottomSheet" to sheet, "topSheet" to header))
            com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeViewControllerBinding::class.java.getField(field).apply { isAccessible = true }.set(binding, value)
        val home = com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment()
        com.skedgo.tripkit.ui.core.BaseFragment::class.java.getDeclaredField("binding").apply { isAccessible = true }.set(home, binding)
        val mapFragment = mockk<TripKitMapFragment>(relaxed = true)
        val contributor = mockk<TripResultMapContributor>(relaxed = true)
        // Actual font2 landscape geometry: header328, marker145, original segment padding50.
        every { contributor.minimumViewportHeight } returns 245
        every { mapFragment.getContributor() } returns contributor
        for ((field, value) in listOf("map" to mockk<com.google.android.gms.maps.GoogleMap>(relaxed = true),
            "mapFragment" to mapFragment, "bottomSheetBehavior" to behavior))
            home.javaClass.getDeclaredField(field).apply { isAccessible = true }.set(home, value)
        home.observeMapViewport()
        val spec = android.view.View.MeasureSpec.EXACTLY
        repeat(2) {
            parent.measure(android.view.View.MeasureSpec.makeMeasureSpec(2400, spec), android.view.View.MeasureSpec.makeMeasureSpec(954, spec))
            parent.layout(0, 0, 2400, 954)
            parent.viewTreeObserver.dispatchOnGlobalLayout()
        }
        val minimumTop = header.bottom + 2 * context.resources.getDimensionPixelSize(com.skedgo.tripkit.ui.R.dimen.spacing_normal) + 245
        org.junit.Assert.assertTrue("sheet top=${sheet.top}, required=$minimumTop", sheet.top >= minimumTop)
        org.junit.Assert.assertEquals(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED, behavior.state)
    }

}
