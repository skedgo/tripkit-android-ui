package com.skedgo.tripkit.ui

import androidx.fragment.app.DialogFragment
import com.skedgo.tripkit.ui.generic.card.TKUICardViewController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostThemeDialogTest {
    @Test fun `public card defers default theme to host bottomSheetDialogTheme`() {
        assertEquals(0, TKUICardViewController().theme)
    }
    @Test fun `explicit host dialog theme is retained`() {
        val card = TKUICardViewController()
        card.setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog)
        assertEquals(android.R.style.Theme_Material_Dialog, card.theme)
    }
}
