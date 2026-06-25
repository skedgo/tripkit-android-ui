package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import com.skedgo.tripkit.ui.utils.DynamicAppColor
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ActionButtonViewModelTest {

    private lateinit var mockContext: Context
    private lateinit var mockDrawable: Drawable
    private lateinit var actionButton: ActionButton

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockDrawable = mockk(relaxed = true)

        mockkStatic(ContextCompat::class)
        mockkObject(DynamicAppColor)

        every { ContextCompat.getDrawable(any(), any()) } returns mockDrawable
        every { ContextCompat.getColor(any(), any()) } returns Color.BLACK
        every { DynamicAppColor.getAppColors() } returns null // Simulate no app color to fallback
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init with primary button should set primary styles`() {
        actionButton = mockk {
            every { text } returns "Primary Button"
            every { icon } returns R.drawable.ic_launcher
            every { tag } returns "primaryTag"
            every { isPrimary } returns true
            every { useIconTint } returns false
        }

        val viewModel = ActionButtonViewModel(mockContext, actionButton)

        assertEquals("Primary Button", viewModel.title.get())
        assertEquals(mockDrawable, viewModel.icon.get())
        assertEquals("primaryTag", viewModel.tag)
        assertEquals(Color.WHITE, viewModel.iconTint.get())
        assertEquals(Color.TRANSPARENT, viewModel.outlineTint.get())
        assertNotNull(viewModel.background.get())
        assertNotNull(viewModel.backgroundTint.get())

        val backgroundTint = viewModel.backgroundTint.get()
        assertTrue(backgroundTint is ColorStateList)
    }

    @Test
    fun `init with non-primary button should set secondary styles`() {
        actionButton = mockk {
            every { text } returns "Secondary Button"
            every { icon } returns R.drawable.ic_launcher
            every { tag } returns "secondaryTag"
            every { isPrimary } returns false
            every { useIconTint } returns true
        }

        val viewModel = ActionButtonViewModel(mockContext, actionButton)

        assertEquals("Secondary Button", viewModel.title.get())
        assertEquals(mockDrawable, viewModel.icon.get())
        assertEquals("secondaryTag", viewModel.tag)
        assertEquals(Color.BLACK, viewModel.iconTint.get()) // From mocked ContextCompat.getColor
        assertEquals(Color.TRANSPARENT, viewModel.outlineTint.get())
        assertNotNull(viewModel.background.get())
        assertNotNull(viewModel.backgroundTint.get())

        val backgroundTint = viewModel.backgroundTint.get()
        assertTrue(backgroundTint is ColorStateList)
    }

    @Test
    fun `showSpinner should update ObservableBoolean`() {
        actionButton = mockk {
            every { text } returns "Spinner Button"
            every { icon } returns R.drawable.ic_launcher
            every { tag } returns "spinnerTag"
            every { isPrimary } returns false
            every { useIconTint } returns false
        }

        val viewModel = ActionButtonViewModel(mockContext, actionButton)
        viewModel.showSpinner(true)
        assertTrue(viewModel.showSpinner.get())
        viewModel.showSpinner(false)
        assertFalse(viewModel.showSpinner.get())
    }
}
