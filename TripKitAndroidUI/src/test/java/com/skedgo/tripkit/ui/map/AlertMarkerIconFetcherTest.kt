package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.base.MockKTest
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class AlertMarkerIconFetcherTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var picasso: Picasso

    @MockK
    private lateinit var marker: Marker

    @MockK
    private lateinit var realtimeAlert: RealtimeAlert

    private lateinit var alertMarkerIconFetcher: AlertMarkerIconFetcher

    @MockK
    private lateinit var picassoLazy: dagger.Lazy<Picasso>

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { picassoLazy.get() } returns picasso

        alertMarkerIconFetcher = AlertMarkerIconFetcher(resources, picassoLazy)
    }

    @Test
    fun `test call with valid icon name`() {
        val iconName = "test_icon"

        // Create a mock RequestCreator
        val requestCreatorMock = mockk<RequestCreator>(relaxed = true)

        // Allow any base URL, just check the filename
        every { realtimeAlert.remoteIcon() } returns iconName
        every { picasso.load(any<String>()) } returns requestCreatorMock
        every { requestCreatorMock.into(any<Target>()) } just Runs

        alertMarkerIconFetcher.call(marker, realtimeAlert)

        verify { requestCreatorMock.into(any<Target>()) }
    }

    @Test
    fun `test call with null icon name`() {
        every { realtimeAlert.remoteIcon() } returns null

        alertMarkerIconFetcher.call(marker, realtimeAlert)

        verify(exactly = 0) { picasso.load(any<String>()) }
    }
}