package com.skedgo.tripkit.ui.timetables

import androidx.lifecycle.ViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.inject.Provider

class TimetableViewModelFactoryTest {

    @Test
    fun create_returnsTimetableViewModelFromProvider() {
        val expected = mockk<TimetableViewModel>(relaxed = true)
        val provider = mockk<Provider<TimetableViewModel>>()
        every { provider.get() } returns expected

        val factory = TimetableViewModelFactory(provider)
        val created = factory.create(TimetableViewModel::class.java)

        assertSame(expected, created)
    }

    @Test
    fun create_throwsForUnsupportedViewModelClass() {
        val provider = mockk<Provider<TimetableViewModel>>(relaxed = true)
        val factory = TimetableViewModelFactory(provider)

        assertThrows(UnsupportedOperationException::class.java) {
            factory.create(ViewModel::class.java)
        }
    }
}
