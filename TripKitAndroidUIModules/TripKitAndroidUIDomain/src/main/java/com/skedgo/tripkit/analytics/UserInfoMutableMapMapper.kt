package com.skedgo.tripkit.analytics

internal fun UserInfo.toMutableMap(): MutableMap<String, Any> =
    mutableMapOf(
        "choiceSet" to this.choiceSet as Any
    )
