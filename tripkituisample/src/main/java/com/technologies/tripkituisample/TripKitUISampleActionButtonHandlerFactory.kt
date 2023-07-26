package com.technologies.tripkituisample

import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonContainer
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import javax.inject.Inject
import javax.inject.Provider

class TripKitUISampleActionButtonHandlerFactory @Inject constructor(private val handlerProvider: Provider<TripKitUISampleActionButtonHandler>) :
    ActionButtonHandlerFactory {
    override fun createHandler(container: ActionButtonContainer): ActionButtonHandler {
        val handler = handlerProvider.get()
        handler.container = container
        return handler
    }
}