package com.skedgo.tripkit.ui.controller

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeBottomSheetFragment
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment
import com.skedgo.tripkit.ui.controller.trippreviewcontroller.TKUITripPreviewFragment
import com.skedgo.tripkit.ui.trippreview.TripPreviewHeaderFragment
import io.mockk.*
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PreviewSelectionScopeTest {
    @org.junit.After fun tearDown() = unmockkAll()
    private fun owner(fragment: Fragment) {
        every { fragment.viewModelStore } returns ViewModelStore()
        every { fragment.defaultViewModelProviderFactory } returns ViewModelProvider.NewInstanceFactory()
        every { fragment.defaultViewModelCreationExtras } returns CreationExtras.Empty
    }
    private fun parent(child: Fragment, parent: Fragment) {
        mockkObject(child)
        every { child.defaultViewModelProviderFactory } returns ViewModelProvider.NewInstanceFactory()
        every { child.defaultViewModelCreationExtras } returns CreationExtras.Empty
        Fragment::class.java.getDeclaredField("mParentFragment").apply { isAccessible = true }.set(child, parent)
    }
    private fun selection(fragment: Fragment): Any = fragment.javaClass.getDeclaredMethod("getSharedViewModel")
        .apply { isAccessible = true }.invoke(fragment)

    @Test fun `nested public preview and sibling header use the same selection state`() {
        val home = mockk<TKUIHomeViewControllerFragment>(relaxed = true).also(::owner)
        val sheet = mockk<TKUIHomeBottomSheetFragment>(relaxed = true).also(::owner)
        every { sheet.parentFragment } returns home
        every { sheet.requireParentFragment() } returns home
        val preview = TKUITripPreviewFragment()
        val header = TripPreviewHeaderFragment()
        parent(preview, sheet)
        parent(header, home)
        assertSame(selection(header), selection(preview))
    }
    private data class Fixture(val home: TKUIHomeViewControllerFragment, val preview: TKUITripPreviewFragment,
        val header: TripPreviewHeaderFragment, val binding: com.skedgo.tripkit.ui.databinding.FragmentTkuiTripPreviewBinding,
        val pages: MutableList<com.skedgo.tripkit.ui.controller.trippreviewcontroller.TripPreviewPagerAdapterItem>,
        val callback: androidx.viewpager.widget.ViewPager.OnPageChangeListener)

    private fun prepared(): Fixture {
        val home = mockk<TKUIHomeViewControllerFragment>(relaxed = true).also(::owner)
        val sheet = mockk<TKUIHomeBottomSheetFragment>(relaxed = true).also(::owner)
        every { sheet.parentFragment } returns home
        every { sheet.requireParentFragment() } returns home
        val preview = TKUITripPreviewFragment()
        val header = TripPreviewHeaderFragment()
        parent(preview, sheet)
        parent(header, home)
        val context = androidx.appcompat.view.ContextThemeWrapper(androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        val binding = com.skedgo.tripkit.ui.databinding.FragmentTkuiTripPreviewBinding.inflate(android.view.LayoutInflater.from(context))
        com.skedgo.tripkit.ui.core.BaseFragment::class.java.getDeclaredField("binding").apply { isAccessible = true }.set(preview, binding)
        val first = mockk<com.skedgo.tripkit.routing.TripSegment>(relaxed = true)
        val second = mockk<com.skedgo.tripkit.routing.TripSegment>(relaxed = true)
        every { first.segmentId } returns 101L
        first.transportModeId = "walk"
        every { second.segmentId } returns 202L
        second.transportModeId = "pt_pub_bus"
        val pages = mutableListOf(com.skedgo.tripkit.ui.controller.trippreviewcontroller.TripPreviewPagerAdapterItem(0, first),
            com.skedgo.tripkit.ui.controller.trippreviewcontroller.TripPreviewPagerAdapterItem(0, second))
        val pagerAdapter = mockk<com.skedgo.tripkit.ui.controller.trippreviewcontroller.TripPreviewPagerAdapter>(relaxed = true)
        TKUITripPreviewFragment::class.java.getDeclaredField("adapter").apply { isAccessible = true }.set(preview, pagerAdapter)
        every { preview.adapter } returns pagerAdapter
        every { pagerAdapter.pages } returns pages
        every { pagerAdapter.count } returns pages.size
        every { pagerAdapter.getSegmentPositionById(any()) } answers {
            val selection = firstArg<Pair<Long, String>>()
            pages.indexOfFirst { it.tripSegment.segmentId == selection.first }
        }
        every { pagerAdapter.instantiateItem(any<android.view.ViewGroup>(), any()) } returns Fragment()
        every { pagerAdapter.getSegmentByPosition(any()) } answers { pages[firstArg()].tripSegment }
        val listenersField = androidx.viewpager.widget.ViewPager::class.java.getDeclaredField("mOnPageChangeListeners").apply { isAccessible = true }
        val existingCount = (listenersField.get(binding.vpTripPreview) as? List<*>)?.size ?: 0
        preview.javaClass.getDeclaredMethod("setViewPagerListeners").apply { isAccessible = true }.invoke(preview)
        @Suppress("UNCHECKED_CAST")
        val callbacks = androidx.viewpager.widget.ViewPager::class.java.getDeclaredField("mOnPageChangeListeners").apply { isAccessible = true }
            .get(binding.vpTripPreview) as List<androidx.viewpager.widget.ViewPager.OnPageChangeListener>
        return Fixture(home, preview, header, binding, pages, callbacks.drop(existingCount).single())
    }

    @Test fun `card changes publish index zero and rapid switches once to the header owner`() {
        val f = prepared()
        val preview = f.preview
        val model = selection(f.header) as com.skedgo.tripkit.ui.trippreview.TripPreviewSharedViewModel
        val observed = mutableListOf<Pair<Long, String>>()
        val observer = androidx.lifecycle.Observer<Pair<Long, String>> { observed.add(it) }
        model.pageIndex.observeForever(observer)
        try {
            for (position in listOf(0, 1, 0, 0)) {
                androidx.viewpager.widget.ViewPager::class.java.getDeclaredField("mCurItem").apply { isAccessible = true }.setInt(f.binding.vpTripPreview, position)
                f.callback.onPageSelected(position)
            }
            org.junit.Assert.assertEquals(listOf(101L to "walk", 202L to "pt_pub_bus", 101L to "walk"), observed)
            val saved = android.os.Bundle()
            preview.onSaveInstanceState(saved)
            org.junit.Assert.assertEquals(101L, saved.getLong("tkui_selected_segment"))
        } finally { model.pageIndex.removeObserver(observer) }
    }

    @Test fun `header selection changes the card and focuses the same segment without feedback loops`() {
        val f = prepared()
        val model = selection(f.header) as com.skedgo.tripkit.ui.trippreview.TripPreviewSharedViewModel
        f.binding.vpTripPreview.adapter = f.preview.adapter
        val contributor = mockk<com.skedgo.tripkit.ui.tripresult.TripResultMapContributor>(relaxed = true)
        val sheet = mockk<TKUIHomeBottomSheetFragment>(relaxed = true)
        every { sheet.getFragmentByTag(TKUITripPreviewFragment.TAG) } returns f.preview
        every { f.home.bottomSheetFragment } returns sheet
        every { f.preview.isVisible } returns true
        val trip = mockk<com.skedgo.tripkit.routing.Trip>(relaxed = true)
        every { trip.segmentList } returns ArrayList(f.pages.map { it.tripSegment })
        every { f.preview.latestTrip } returns trip
        for ((name, value) in listOf("bottomSheetFragment" to sheet, "previewMapContributor" to contributor))
            TKUIHomeViewControllerFragment::class.java.getDeclaredField(name).apply { isAccessible = true }.set(f.home, value)
        every { f.home.focusPreviewSegment(any()) } answers { callOriginal() }
        val observer = androidx.lifecycle.Observer<Pair<Long, String>> { f.home.focusPreviewSegment(it.first) }
        model.pageIndex.observeForever(observer)
        clearMocks(contributor, answers = false)
        try {
            model.setPageIndex(202L, "pt_pub_bus")
            f.preview.selectHeaderSegment(202L to "pt_pub_bus")
            org.junit.Assert.assertEquals(1, f.binding.vpTripPreview.currentItem)
            verify(exactly = 1) { contributor.focusTripLine(f.pages[1].tripSegment) }
            model.setPageIndex(101L, "walk")
            f.preview.selectHeaderSegment(101L to "walk")
            org.junit.Assert.assertEquals(0, f.binding.vpTripPreview.currentItem)
            verify(exactly = 1) { contributor.focusTripLine(f.pages[0].tripSegment) }
        } finally { model.pageIndex.removeObserver(observer) }
    }
    private class ViewOwner : androidx.lifecycle.LifecycleOwner {
        val registry = androidx.lifecycle.LifecycleRegistry(this)
        override val lifecycle: androidx.lifecycle.Lifecycle get() = registry
    }

    @Test fun `selection reconnects after cached view lifecycle is recreated`() {
        val f = prepared()
        f.binding.vpTripPreview.adapter = f.preview.adapter
        val model = selection(f.header) as com.skedgo.tripkit.ui.trippreview.TripPreviewSharedViewModel
        val firstOwner = ViewOwner().apply { registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        every { f.preview.viewLifecycleOwner } returns firstOwner
        f.preview.onResume()
        model.setPageIndex(202L, "pt_pub_bus")
        org.junit.Assert.assertEquals(1, f.binding.vpTripPreview.currentItem)
        firstOwner.registry.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
        model.setPageIndex(101L, "walk")
        org.junit.Assert.assertEquals(1, f.binding.vpTripPreview.currentItem)
        val secondOwner = ViewOwner().apply { registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        every { f.preview.viewLifecycleOwner } returns secondOwner
        f.preview.onResume()
        org.junit.Assert.assertEquals(0, f.binding.vpTripPreview.currentItem)
        f.preview.onResume()
        model.setPageIndex(202L, "pt_pub_bus")
        org.junit.Assert.assertEquals(1, f.binding.vpTripPreview.currentItem)
        secondOwner.registry.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

}
