package com.skedgo.tripkit.ui.controller.routeviewcontroller

import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiRouteComposeBinding
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class RouteInputFocusRegressionTest {
    private lateinit var activity: ActivityController<ComponentActivity>
    private lateinit var binding: FragmentTkuiRouteComposeBinding
    private lateinit var route: TKUIRouteFragment
    private lateinit var model: TKUIRouteViewModel
    private val store = ViewModelStore()
    private val setup = TKUIRouteFragment::class.java.getDeclaredMethod("setupRouteCompose")
        .apply { isAccessible = true }

    @Before fun setUp() {
        // Advance vsync explicitly; cursor blinking otherwise keeps the looper draining forever.
        org.robolectric.shadows.ShadowChoreographer.setPaused(true)
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
        activity = Robolectric.buildActivity(ComponentActivity::class.java).create()
        activity.get().setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar)
        binding = FragmentTkuiRouteComposeBinding.inflate(activity.get().layoutInflater)
        activity.get().setContentView(binding.root)
        activity.start().resume().visible()
        route = TKUIRouteFragment().apply { eventBus = ViewControllerEventBus }
        mockkObject(route)
        every { route.resources } returns activity.get().resources
        every { route.viewModelStore } returns store
        every { route.defaultViewModelProviderFactory } returns ViewModelProvider.NewInstanceFactory()
        every { route.defaultViewModelCreationExtras } returns CreationExtras.Empty
        BaseFragment::class.java.getDeclaredField("binding").apply { isAccessible = true }.set(route, binding)
        TKUIRouteFragment::class.java.getDeclaredField("locationSearchFragment").apply { isAccessible = true }
            .set(route, mockk<TKUILocationSearchViewControllerFragment>(relaxed = true))
        model = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[TKUIRouteViewModel::class.java]
        setup.invoke(route)
        frame()
        binding.root.dispatchWindowFocusChanged(true)
        frame()
    }

    @After fun tearDown() {
        binding.root.clearFocus()
        binding.routeCompose.disposeComposition()
        store.clear()
        activity.pause().stop().destroy()
        unmockkAll()
    }

    private fun frame() {
        shadowOf(Looper.getMainLooper()).idleFor(32, TimeUnit.MILLISECONDS)
        binding.root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1400, View.MeasureSpec.EXACTLY))
        binding.root.layout(0, 0, 1080, 1400)
        shadowOf(Looper.getMainLooper()).idleFor(32, TimeUnit.MILLISECONDS)
    }

    private fun fields(): List<SemanticsNode> {
        val owner = binding.routeCompose.getChildAt(0)
        val semantics = owner.javaClass.getMethod("getSemanticsOwner").invoke(owner) as SemanticsOwner
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return descendants(semantics.unmergedRootSemanticsNode)
            .filter { it.config.contains(SemanticsProperties.EditableText) }
    }

    private fun focus(index: Int): android.view.inputmethod.InputConnection {
        assertEquals(2, fields().size)
        assertTrue(fields()[index].config[SemanticsActions.RequestFocus].action!!.invoke())
        frame()
        assertTrue("Focus immediately after request: ${fields().map { it.id to it.config[SemanticsProperties.Focused] }}", fields()[index].config[SemanticsProperties.Focused])
        return requireNotNull(binding.routeCompose.getChildAt(0).onCreateInputConnection(EditorInfo()))
    }

    @Test fun `route editors preserve continuous input cursor identity and independent fields`() {
        val input = focus(0)
        var typed = ""
        for (character in "Town Hall") {
            typed += character
            assertTrue(input.commitText(character.toString(), 1))
            // No main-loop drain: the parent must accept each edit synchronously.
            assertEquals(typed, model.start.value)
            frame()
            assertTrue("Focus state: ${fields().map { it.id to it.config[SemanticsProperties.Focused] }} model=${model.start.value}", fields()[0].config[SemanticsProperties.Focused])
            assertEquals(typed.length, fields()[0].config[SemanticsProperties.TextSelectionRange].start)
        }
        input.setSelection(4, 4)
        input.commitText("X", 1)
        frame()
        assertEquals("TownX Hall", model.start.value)
        assertEquals(5, fields()[0].config[SemanticsProperties.TextSelectionRange].start)
        checkIndependentEditors()
        checkReconnectedHeader()
    }

    private fun checkIndependentEditors() {
        model.setStart("")
        model.setDestination("")
        frame()
        focus(0).commitText("Town Hall", 1)
        frame()
        focus(1).commitText("Bondi Beach", 1)
        assertEquals("Town Hall", model.start.value)
        assertEquals("Bondi Beach", model.destination.value)
        assertEquals(TKUIRouteViewModel.FocusedField.DESTINATION, model.focusedField)
        frame()
        assertTrue(fields()[1].config[SemanticsProperties.Focused])
        assertFalse(fields()[0].config[SemanticsProperties.Focused])
    }

    private fun checkReconnectedHeader() {
        model.setStart("")
        frame()
        val input = focus(0)
        input.commitText("Town Hall", 1)
        frame()
        input.setSelection(4, 4)
        frame()
        val owner = binding.routeCompose.getChildAt(0)
        val fieldId = fields()[0].id
        setup.invoke(route)
        frame()
        assertSame(owner, binding.routeCompose.getChildAt(0))
        assertEquals(fieldId, fields()[0].id)
        assertTrue("Focus state: ${fields().map { it.id to it.config[SemanticsProperties.Focused] }} model=${model.start.value}", fields()[0].config[SemanticsProperties.Focused])
        assertEquals(4, fields()[0].config[SemanticsProperties.TextSelectionRange].start)
    }
}
