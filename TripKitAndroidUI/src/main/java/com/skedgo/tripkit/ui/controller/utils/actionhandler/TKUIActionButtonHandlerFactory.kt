package com.skedgo.tripkit.ui.controller.utils.actionhandler

import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonContainer
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import javax.inject.Inject
import javax.inject.Provider

class TKUIActionButtonHandlerFactory @Inject constructor(private val handlerProvider: Provider<TKUIActionButtonHandler>) :
    ActionButtonHandlerFactory {
    override fun createHandler(container: ActionButtonContainer): ActionButtonHandler {
        val handler = handlerProvider.get()
        handler.container = container
        return handler
    }
}