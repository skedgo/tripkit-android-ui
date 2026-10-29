package com.skedgo.tripkit.ui.utils

import android.content.Context
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.tripresults.PrefsBasedTransportViewFilter
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.tripresult.TripSegmentItemViewModel
import com.skedgo.tripkit.ui.tripresults.TripSegmentViewModel
import io.mockk.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceAccessibilityViewsTest : MockKTest() {
    @get:Rule val rule = InstantTaskExecutorRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = DefaultTripPreferences(context.getSharedPreferences("TripPreferences", 0))
    private lateinit var summary: TripSegmentViewModel
    private lateinit var detail: TripSegmentItemViewModel
    private val trip = mockk<Trip>(relaxed = true)

    @Before fun setup() {
        initRx()
        context.getSharedPreferences("TripPreferences", 0).edit().clear().commit()
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, 0).edit().clear().commit()
        mockkObject(TripKitUI.Companion)
        every { TripKitUI.getInstance() } returns mockk(relaxed = true)
        summary = TripSegmentViewModel(context, mockk(relaxed = true), TransportModeSharedPreference(context))
        detail = TripSegmentItemViewModel(context, mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), TransportModeSharedPreference(context))
    }

    @After fun teardown() {
        summary.onCleared()
        detail.onCleared()
        unmockkAll()
        tearDownRx()
    }

    private fun bind(status: Boolean?) {
        val segment = TripSegment().apply {
            setType(SegmentType.SCHEDULED)
            wheelchairAccessible = status
            bicycleAccessible = true
        }
        summary.setSegment(trip, segment)
        detail.tripSegment = segment
        detail.setupSegment(TripSegmentItemViewModel.SegmentViewType.MOVING, "Canberra service")
    }

    @Test fun `summary and detailed trip rows agree for all wheelchair states`() {
        for (information in listOf(true, false)) {
            preferences.setWheelchairPreferred(information)
            for (status in listOf(true, false, null)) {
                bind(status)
                val visible = status == false || information
                Assert.assertEquals(visible, summary.wheelchairIcon.value != null)
                Assert.assertEquals(visible, detail.wheelchairIcon.value != null)
            }
        }
    }

    @Test fun `preference changes refresh both trip rows without rebinding`() {
        bind(null)
        Assert.assertNull(summary.wheelchairIcon.value)
        Assert.assertNull(detail.wheelchairIcon.value)
        val filter = PrefsBasedTransportViewFilter(context)
        filter.setSelected(TransportMode.ID_WALK, false)
        Assert.assertNotNull(summary.wheelchairIcon.value)
        Assert.assertNotNull(detail.wheelchairIcon.value)
        Assert.assertTrue(preferences.isWheelchairPreferred())
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, false)
        Assert.assertNull(summary.wheelchairIcon.value)
        Assert.assertNull(detail.wheelchairIcon.value)
        preferences.setWheelchairPreferred(true)
        Assert.assertNotNull(summary.wheelchairIcon.value)
        Assert.assertNotNull(detail.wheelchairIcon.value)
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, 0).edit().putBoolean("cy_bic", false).commit()
        Assert.assertEquals(false, summary.isBicycleAccessible.value)
        Assert.assertEquals(false, detail.showBicycleAccessible.value)
    }

    @Test fun `recycled detailed row clears icons for stationary and absent segments`() {
        preferences.setWheelchairPreferred(true)
        bind(true)
        Assert.assertNotNull(detail.wheelchairIcon.value)
        detail.setupSegment(TripSegmentItemViewModel.SegmentViewType.STATIONARY, "Stop", isStationaryItem = true)
        Assert.assertNull(detail.wheelchairIcon.value)
        Assert.assertEquals(false, detail.showBicycleAccessible.value)
        detail.tripSegment = null
        detail.setupSegment(TripSegmentItemViewModel.SegmentViewType.MOVING, "Empty")
        Assert.assertNull(detail.wheelchairIcon.value)
        Assert.assertEquals(false, detail.showBicycleAccessible.value)
    }
    @Test fun `rendered summary places wheelchair before bicycle`() {
        preferences.setWheelchairPreferred(true)
        bind(true)
        val binding = com.skedgo.tripkit.ui.databinding.TripResultSegmentItemBinding.inflate(
            android.view.LayoutInflater.from(context))
        binding.viewModel = summary
        binding.executePendingBindings()
        val root = binding.root as android.view.ViewGroup
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(2400, android.view.View.MeasureSpec.AT_MOST))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val wheelchair = root.findViewById<android.widget.ImageView>(com.skedgo.tripkit.ui.R.id.wheelchair)
        val bicycle = root.getChildAt(root.indexOfChild(wheelchair) + 1) as android.widget.ImageView
        Assert.assertEquals(android.view.View.VISIBLE, wheelchair.visibility)
        Assert.assertEquals(android.view.View.VISIBLE, bicycle.visibility)
        Assert.assertNotNull(wheelchair.drawable)
        Assert.assertNotNull(bicycle.drawable)
        Assert.assertTrue("Wheelchair must precede bike", wheelchair.right <= bicycle.left)
    }

    @Test fun `inflated service summary observes preference changes through view lifecycle`() {
        bind(null)
        val group = android.widget.LinearLayout(context)
        val owner = object : androidx.lifecycle.LifecycleOwner {
            val registry = androidx.lifecycle.LifecycleRegistry(this)
            override val lifecycle: androidx.lifecycle.Lifecycle get() = registry
        }
        owner.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED
        group.setViewTreeLifecycleOwner(owner)
        com.skedgo.tripkit.ui.core.binding.setEntries(group, listOf(summary),
            com.skedgo.tripkit.ui.R.layout.trip_result_segment_item)
        val binding = androidx.databinding.DataBindingUtil.getBinding<androidx.databinding.ViewDataBinding>(group.getChildAt(0))!!
        binding.executePendingBindings()
        val wheelchair = group.findViewById<android.view.View>(com.skedgo.tripkit.ui.R.id.wheelchair)
        Assert.assertEquals(android.view.View.GONE, wheelchair.visibility)
        preferences.setWheelchairPreferred(true)
        binding.executePendingBindings()
        Assert.assertEquals(android.view.View.VISIBLE, wheelchair.visibility)
        owner.registry.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
    }


    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `unknown and inaccessible wheelchair glyphs render visibly in light and dark trip rows`() {
        preferences.setWheelchairPreferred(true)
        for (night in listOf(false, true)) {
            val configuration = android.content.res.Configuration(context.resources.configuration).apply {
                uiMode = if (night) android.content.res.Configuration.UI_MODE_NIGHT_YES
                    else android.content.res.Configuration.UI_MODE_NIGHT_NO
            }
            val themed = android.view.ContextThemeWrapper(context.createConfigurationContext(configuration), com.skedgo.tripkit.ui.R.style.AppTheme)
            summary.onCleared()
            detail.onCleared()
            summary = TripSegmentViewModel(themed, mockk(relaxed = true), TransportModeSharedPreference(themed))
            detail = TripSegmentItemViewModel(themed, mockk(relaxed = true), mockk(relaxed = true),
                mockk(relaxed = true), com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel(themed), TransportModeSharedPreference(themed))
            val surface = themed.getColor(com.skedgo.tripkit.ui.R.color.white)
            for (status in listOf<Boolean?>(null, false)) {
                bind(status)
                detail.icon.value = null // The transport-image provider is mocked; test real accessibility drawables.
                val summaryBinding = com.skedgo.tripkit.ui.databinding.TripResultSegmentItemBinding.inflate(android.view.LayoutInflater.from(themed))
                summaryBinding.viewModel = summary
                summaryBinding.executePendingBindings()
                val detailBinding = com.skedgo.tripkit.ui.databinding.TripSegmentBinding.inflate(android.view.LayoutInflater.from(themed))
                detailBinding.viewModel = detail
                detailBinding.executePendingBindings()
                val images = listOf(summaryBinding.root.findViewById<android.widget.ImageView>(com.skedgo.tripkit.ui.R.id.wheelchair),
                    detailBinding.root.findViewById<android.widget.ImageView>(com.skedgo.tripkit.ui.R.id.ivWheelchairAccessible))
                for (image in images) {
                    val bitmap = android.graphics.Bitmap.createBitmap(160, 160, android.graphics.Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(surface)
                    image.measure(android.view.View.MeasureSpec.makeMeasureSpec(160, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(160, android.view.View.MeasureSpec.EXACTLY))
                    image.layout(0, 0, 160, 160)
                    image.draw(android.graphics.Canvas(bitmap))
                    val pixels = IntArray(160 * 160)
                    bitmap.getPixels(pixels, 0, 160, 0, 0, 160, 160)
                    val visible = pixels.count { androidx.core.graphics.ColorUtils.calculateContrast(it, surface) >= 3.0 }
                    Assert.assertTrue("status=$status night=$night must render a readable glyph, visible pixels=$visible", visible > 500)
                }
            }
        }
    }
    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `service bicycle icons preserve selected drawable colors without tint in both themes`() {
        for (night in listOf(false, true)) {
            val configuration = android.content.res.Configuration(context.resources.configuration).apply {
                uiMode = if (night) android.content.res.Configuration.UI_MODE_NIGHT_YES
                    else android.content.res.Configuration.UI_MODE_NIGHT_NO
            }
            val themed = android.view.ContextThemeWrapper(context.createConfigurationContext(configuration), com.skedgo.tripkit.ui.R.style.AppTheme)
            val binding = com.skedgo.tripkit.ui.databinding.ServiceDetailFragmentContentBinding.inflate(android.view.LayoutInflater.from(themed))
            val expanded = (binding.tvBicycleLabel.parent as android.view.ViewGroup).getChildAt(0) as android.widget.ImageView
            for (image in listOf(binding.ivBicycle, expanded)) {
                image.visibility = android.view.View.VISIBLE
                Assert.assertNull("night=$night must preserve provider artwork", image.imageTintList)
                Assert.assertNull("night=$night must not apply a color filter", image.colorFilter)
                val expected = android.widget.ImageView(themed).apply {
                    setImageResource(com.skedgo.tripkit.ui.R.drawable.ic_bike)
                }
                val original = renderIcon(expected)
                Assert.assertTrue("The selected bicycle drawable must contain a glyph",
                    original.count { android.graphics.Color.alpha(it) > 0 } > 500)
                Assert.assertArrayEquals("night=$night must display the original ic_bike artwork",
                    original, renderIcon(image))

                // White-label hosts such as MyWay+ override ic_bike with multicolored artwork.
                // Exercise the same ImageViews with a palette so a blanket tint cannot pass.
                val palette = intArrayOf(0xFF0045AB.toInt(), 0xFFC00020.toInt(), 0xFF3D2070.toInt(), android.graphics.Color.WHITE)
                fun artwork() = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, palette)
                image.setImageDrawable(artwork())
                expected.setImageDrawable(artwork())
                val providerPixels = renderIcon(expected)
                Assert.assertTrue(providerPixels.toSet().size > 3)
                Assert.assertArrayEquals("night=$night must retain multicolored provider artwork",
                    providerPixels, renderIcon(image))
            }
        }
    }

    private fun renderIcon(image: android.widget.ImageView): IntArray {
        val bitmap = android.graphics.Bitmap.createBitmap(160, 160, android.graphics.Bitmap.Config.ARGB_8888)
        image.measure(android.view.View.MeasureSpec.makeMeasureSpec(160, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(160, android.view.View.MeasureSpec.EXACTLY))
        image.layout(0, 0, 160, 160)
        image.draw(android.graphics.Canvas(bitmap))
        return IntArray(160 * 160).also { bitmap.getPixels(it, 0, 160, 0, 0, 160, 160) }
    }

}
