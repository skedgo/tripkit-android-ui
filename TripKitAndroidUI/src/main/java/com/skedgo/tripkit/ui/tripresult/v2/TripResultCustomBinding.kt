package com.skedgo.tripkit.ui.tripresult.v2

import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2

@BindingAdapter("currentItem")
fun ViewPager2.setCurrentItemBinding(currentItem: Int?) {
    currentItem?.let {
        if (this.currentItem != it) {
            setCurrentItem(it, false)
        }
    }
}
