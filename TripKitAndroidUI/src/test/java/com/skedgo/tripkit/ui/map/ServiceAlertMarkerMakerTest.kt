package com.skedgo.tripkit.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceAlertMarkerMakerTest {

    @MockK(relaxed = true)
    lateinit var context: Context

    @MockK(relaxed = true)
    lateinit var alert: RealtimeAlert

    @MockK(relaxed = true)
    lateinit var drawable: Drawable

    @MockK(relaxed = true)
    lateinit var location: Location

    private lateinit var serviceAlertMarkerMaker: ServiceAlertMarkerMaker

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        serviceAlertMarkerMaker = ServiceAlertMarkerMaker(context)

        // Mock alert properties
        every { alert.severity() } returns RealtimeAlert.SEVERITY_ALERT
        every { alert.title() } returns "Service Disruption"

        // Mock Location properties
        every { location.lat } returns 37.7749
        every { location.lon } returns -122.4194
        every { alert.location() } returns location

        // Mock Drawable behavior
        every { ContextCompat.getDrawable(context, R.drawable.ic_alert_red_overlay) } returns drawable
        every { drawable.intrinsicWidth } returns 50
        every { drawable.intrinsicHeight } returns 50

        val mockRect = mockk<Rect>(relaxed = true)
        every { drawable.bounds } returns mockRect

        every { drawable.setBounds(0, 0, 50, 50) } just Runs

        // **Fix Bitmap.createBitmap Mocking**
        mockkStatic(Bitmap::class)
        val mockBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
        every { Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888) } returns mockBitmap

        // Mock Canvas behavior
        mockkConstructor(Canvas::class)
        every { anyConstructed<Canvas>().drawBitmap(any(), any<Float>(), any<Float>(), any()) } just Runs
        every { drawable.draw(any()) } just Runs

        // Mock BitmapDescriptorFactory (avoid real calls)
        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.fromBitmap(any()) } returns mockk()
    }

    @Test
    fun `make should return MarkerOptions with correct values`() {
        // Act
        val result = serviceAlertMarkerMaker.make(alert)

        // Assert
        assertNotNull(result)
        assertEquals("Service Disruption", result.title)
        assertEquals(
            LatLng(37.7749, -122.4194).latitude,
            result.position.latitude
        )
        assertEquals(
            LatLng(37.7749, -122.4194).longitude,
            result.position.longitude
        )
        assertEquals(false, result.isDraggable)
    }
}