package com.skedgo.tripkit.ui.core.binding

import android.content.res.ColorStateList
import androidx.databinding.BindingAdapter
import com.google.android.material.button.MaterialButton

// With the Material library 1.2.1, binding breaks when setting backgroundTint to a ColorStateList. It's possible that
// it's just a bug with the library, so this workaround may no longer be necessary when you read this.
@BindingAdapter("backgroundTintBinding")
fun backgroundTintBinding(button: MaterialButton, colorStateList: ColorStateList) {
    button.backgroundTintList = colorStateList
}
