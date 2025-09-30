package com.skedgo.tripkit.ui.generic.card

import androidx.fragment.app.Fragment
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.TripKitConfigs
import com.skedgo.tripkit.ui.generic.bottomsheet.TKUICardHost

interface TKUICardNavigator {

    val bottomSheetManager: TKUICardHost?

    val parentFragment: Fragment?

    val configs: Configs?

}