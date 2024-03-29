package com.skedgo.tripkit.ui.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow


class TapStateFlow<TSender> internal constructor(
        private val getSender: () -> TSender
) {
    companion object Factory {
        fun <TSender> create(getSender: () -> TSender): TapStateFlow<TSender>
                = TapStateFlow(getSender)
    }

    private val onTap = Channel<TSender>()
    val observable = onTap.receiveAsFlow()

    fun perform() {
        onTap.trySend(getSender()).isSuccess
    }
}
