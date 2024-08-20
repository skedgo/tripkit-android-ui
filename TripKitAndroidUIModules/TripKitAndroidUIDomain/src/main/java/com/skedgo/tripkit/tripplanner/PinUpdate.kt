package com.skedgo.tripkit.tripplanner

import io.reactivex.functions.Action
import io.reactivex.functions.Consumer

sealed class PinUpdate {
    object Delete : PinUpdate()
    data class Create(val type: NonCurrentType) : PinUpdate()

    /**
     * This is to avoid `instanceof` if we use [PinUpdate] with Java.
     */
    fun match(
        onDelete: Action,
        onCreate: Consumer<Create>
    ) = when (this) {
        Delete -> onDelete.run()
        is Create -> onCreate.accept(this)
    }
}
