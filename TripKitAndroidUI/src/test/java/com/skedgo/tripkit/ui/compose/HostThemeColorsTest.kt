package com.skedgo.tripkit.ui.compose

import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.ui.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostThemeColorsTest {
    @Test fun `host brand wins over SDK resource default`() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(),
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
        val primary = context.hostThemeColor(androidx.appcompat.R.attr.colorPrimary, R.color.colorPrimary)
        assertEquals(com.google.android.material.color.MaterialColors.getColor(context,
            androidx.appcompat.R.attr.colorPrimary, "test"), primary)
        assertNotEquals(ContextCompat.getColor(context, R.color.colorPrimary), primary)
    }

    @Test fun `legacy host missing an optional attribute uses SDK fallback`() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), android.R.style.Theme_Material_Light_NoActionBar)
        assertEquals(ContextCompat.getColor(context, R.color.tripKitError),
            context.hostThemeColor(com.google.android.material.R.attr.colorOnError, R.color.tripKitError))
    }
}
