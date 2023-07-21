package com.skedgo.tripkit.ui.model

data class UserMode(
    var mode: String? = null,
    val rules: UserModeRule? = null
)

data class UserModeRule(
    var replaceWith: List<String>? = null,
    val rules: UserModeRule? = null
)