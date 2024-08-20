package com.skedgo.tripkit.ui.utils

// We have to support API 19, so we can't use java.util.Optional
data class Optional<M>(val value: M?)