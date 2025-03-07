package com.skedgo.tripkit.ui.trippreview.directions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import com.skedgo.tripkit.common.model.Street
import com.skedgo.tripkit.routing.RoadTag
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.trippreview.GetInstructionIcon
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DirectionsTripPreviewItemViewModelTest {

    @After
    fun teardown() {
        clearAllMocks()
    }


    @Test
    fun `generateRoadTagItems filters UNKNOWN and maps road tags correctly`() {
        val stepViewModel = DirectionsTripPreviewItemStepViewModel()
        stepViewModel.roadTags = listOf(RoadTag.BICYCLE_DESIGNATED, RoadTag.UNKNOWN, RoadTag.SIDE_WALK)

        val items = stepViewModel.generateRoadTagItems()

        assertEquals(2, items.size) // UNKNOWN should be filtered out
        assertEquals("Designated for Cyclists", items[0].label)
        assertEquals("Side Walk", items[1].label)
    }

    @Ignore("Inconsistent, to check later")
    @Test
    fun `setSegment initializes items correctly`() {

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        mockkStatic(GetInstructionIcon::class)
        mockkStatic(AppCompatResources::class)
        mockkStatic(TextUtils::class)
        mockkStatic(DateUtils::class)
        mockkStatic(DistanceFormatter::class)
        mockkStatic(DateTimeZone::class)

        every { TextUtils.isEmpty(any()) } answers { false }
        every { DateUtils.isToday(any()) } answers { false }
        every { DateTimeZone.forID(any()) } answers { DateTimeZone.UTC }
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk()

        val context: Context = mockk(relaxed = true)
        val iconGetter: GetInstructionIcon = mockk(relaxed = true)
        val viewModel = DirectionsTripPreviewItemViewModel()
        val segment = mockk<TripSegment>(relaxed = true)
        val street = mockk<Street>(relaxed = true)

        every { segment.streets } returns listOf(street)
        every { street.metres() } returns 1000.0f
        every { street.name() } returns "Main St"
        every { street.roadTags() } returns listOf("BIKE_LANE", "PEDESTRIAN")
        every { segment.timeZone } returns "Australia/Sydney"
        every { iconGetter.getIcon(context, any()) } returns mockk()
        every { context.getString(any(), any()) } answers {
            val resId = firstArg<Int>()
            val formatArgs = secondArg<Array<Any>>()
            "Along ${formatArgs.joinToString()}"
        }

        val mockDrawable: Drawable = mockk(relaxed = true)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockDrawable
        every { mockDrawable.setTint(any()) } returns Unit

        viewModel.setSegment(context, segment)

        assertEquals(1, viewModel.items.size)
        val stepItem = viewModel.items.first()
        assertEquals("0.6 mi", stepItem.title.get())
        assertEquals("Along Main St", stepItem.description.get())
        assertNotNull(stepItem.icon.get())
        assertEquals(2, stepItem.roadTags.size)
    }

}