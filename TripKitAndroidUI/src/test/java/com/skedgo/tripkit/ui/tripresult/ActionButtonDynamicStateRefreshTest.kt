package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.utils.DynamicAppColor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the Trip Details notification controls staying in sync (follow-up to
 * Redmine #26114 / NATV-399).
 *
 * The top Alert Me/Mute action and the bottom Trip Notifications switch both derive from
 * [com.skedgo.tripkit.routing.GetOffAlertCache]. Dynamic-state preservation added for #26008
 * stops a realtime trip refresh from reverting a freshly toggled action, but it must not stop
 * the action from picking up a state change that has actually happened - otherwise the switch
 * turns on while the top action keeps saying "Alert Me".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ActionButtonDynamicStateRefreshTest {

    private lateinit var context: Context
    private lateinit var alertMeIcon: Drawable
    private lateinit var muteIcon: Drawable

    private val alertTag = ActionButtonHandler.ACTION_TAG_ALERT
    private val favoriteTag = ActionButtonHandler.ACTION_TAG_FAVORITE

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        alertMeIcon = mockk(relaxed = true)
        muteIcon = mockk(relaxed = true)

        mockkStatic(ContextCompat::class)
        mockkObject(DynamicAppColor)
        every { DynamicAppColor.getAppColors() } returns null
        every { ContextCompat.getColor(any(), any()) } returns Color.BLACK
        every { ContextCompat.getDrawable(context, R.drawable.ic_launcher) } returns alertMeIcon
        every { ContextCompat.getDrawable(context, R.drawable.ic_share) } returns muteIcon
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun alertButton(text: String, icon: Int) = ActionButton(
        text = text,
        tag = alertTag,
        icon = icon,
        isPrimary = false,
        useIconTint = false
    )

    // --- the preservation decision -------------------------------------------------------

    @Test
    fun `alert action is refreshed when its own state just changed`() {
        assertThat(
            TripSegmentsViewModel.shouldPreserveDynamicState(alertTag, alertTag)
        ).isFalse()
    }

    @Test
    fun `alert action is preserved during an unrelated refresh`() {
        assertThat(
            TripSegmentsViewModel.shouldPreserveDynamicState(alertTag, null)
        ).isTrue()
    }

    @Test
    fun `favorite action keeps its state while the alert action is refreshed`() {
        assertThat(
            TripSegmentsViewModel.shouldPreserveDynamicState(favoriteTag, alertTag)
        ).isTrue()
    }

    @Test
    fun `non dynamic actions are never preserved`() {
        assertThat(
            TripSegmentsViewModel.shouldPreserveDynamicState(
                ActionButtonHandler.ACTION_TAG_SHARE, alertTag
            )
        ).isFalse()
    }

    // --- what the button actually renders ------------------------------------------------

    @Test
    fun `enabling notifications refreshes the action to Mute with its icon`() {
        val viewModel = ActionButtonViewModel(
            context, alertButton("Alert Me", R.drawable.ic_launcher)
        )

        viewModel.update(
            context,
            alertButton("Mute", R.drawable.ic_share),
            preserveDynamicState =
                TripSegmentsViewModel.shouldPreserveDynamicState(alertTag, alertTag)
        )

        assertThat(viewModel.title.get()).isEqualTo("Mute")
        assertThat(viewModel.icon.get()).isSameAs(muteIcon)
    }

    @Test
    fun `disabling notifications refreshes the action back to Alert Me`() {
        val viewModel = ActionButtonViewModel(
            context, alertButton("Mute", R.drawable.ic_share)
        )

        viewModel.update(
            context,
            alertButton("Alert Me", R.drawable.ic_launcher),
            preserveDynamicState =
                TripSegmentsViewModel.shouldPreserveDynamicState(alertTag, alertTag)
        )

        assertThat(viewModel.title.get()).isEqualTo("Alert Me")
        assertThat(viewModel.icon.get()).isSameAs(alertMeIcon)
    }

    @Test
    fun `a realtime refresh does not revert a freshly toggled action`() {
        val viewModel = ActionButtonViewModel(
            context, alertButton("Mute", R.drawable.ic_share)
        )

        // A realtime trip update names no changed action, so stale metadata must not win.
        viewModel.update(
            context,
            alertButton("Alert Me", R.drawable.ic_launcher),
            preserveDynamicState =
                TripSegmentsViewModel.shouldPreserveDynamicState(alertTag, null)
        )

        assertThat(viewModel.title.get()).isEqualTo("Mute")
        assertThat(viewModel.icon.get()).isSameAs(muteIcon)
    }
}
