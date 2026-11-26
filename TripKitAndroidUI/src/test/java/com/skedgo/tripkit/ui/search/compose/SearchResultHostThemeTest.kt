package com.skedgo.tripkit.ui.search.compose

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.search.FixedSuggestionViewModel
import com.skedgo.tripkit.ui.search.SearchSuggestion
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchResultHostThemeTest {
    private fun context(night: Boolean): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(app.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return app.createConfigurationContext(config)
    }

    private fun suggestion(source: Context, titleColor: Int = R.color.title_text): FixedSuggestionViewModel {
        val suggestion = mockk<SearchSuggestion>(relaxed = true)
        every { suggestion.id() } returns "favorite"
        every { suggestion.title() } returns "Favorite"
        every { suggestion.subtitle() } returns "Address"
        every { suggestion.titleColor() } returns titleColor
        every { suggestion.subtitleColor() } returns R.color.description_text
        every { suggestion.icon() } returns android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        every { suggestion.location() } returns null
        return FixedSuggestionViewModel(source, suggestion)
    }

    @Test fun `light host renders readable rows even when application is dark`() {
        val dark = context(true)
        val light = context(false)
        val vm = suggestion(dark)
        val row = mapSearchResultRows(listOf(vm), light).single()
        assertNotEquals(vm.titleTextColor, row.titleTextColor)
        assertEquals(ContextCompat.getColor(light, R.color.title_text), row.titleTextColor)
        assertEquals(ContextCompat.getColor(light, R.color.description_text), row.subtitleTextColor)
    }

    @Test fun `dark host and runtime appearance change resolve fresh resource colors`() {
        val vm = suggestion(context(false))
        val dark = mapSearchResultRows(listOf(vm), context(true)).single()
        val light = mapSearchResultRows(listOf(vm), context(false)).single()
        assertNotEquals(light.titleTextColor, dark.titleTextColor)
    }

    @Test fun `provider custom semantic color is preserved`() {
        val host = context(false)
        val row = mapSearchResultRows(listOf(suggestion(context(true), R.color.tripKitSuccess)), host).single()
        assertEquals(ContextCompat.getColor(host, R.color.tripKitSuccess), row.titleTextColor)
    }
    @Test fun `history group uses the displaying host configuration too`() {
        val source = context(true)
        val vm = com.skedgo.tripkit.ui.search.HistorySearchProviderSuggestionViewModel(source, suggestion(source).suggestion)
        val host = context(false)
        val row = mapSearchResultRows(listOf(vm), host).single()
        assertEquals(SearchResultGroupKind.HISTORY, row.groupKind)
        assertEquals(ContextCompat.getColor(host, R.color.title_text), row.titleTextColor)
    }

}
