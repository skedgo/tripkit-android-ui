package com.skedgo.tripkit.ui.tripresults.actionbutton

import kotlinx.coroutines.CoroutineScope

interface ActionButtonHandlerFactory {
    fun createHandler(container: ActionButtonContainer): ActionButtonHandler
}