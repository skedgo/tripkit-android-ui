package com.skedgo.tripkit.ui.core.binding
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.InverseBindingMethod
import androidx.databinding.InverseBindingMethods
import androidx.databinding.adapters.ListenerUtil
import androidx.viewpager.widget.ViewPager
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tripresult.TripGroupsPagerAdapter

import com.skedgo.tripkit.routing.TripGroup

@InverseBindingMethods(InverseBindingMethod(
    type = ViewPager::class,
    attribute = "currentItem",
    method = "getCurrentItem"
))
object ViewPageCurrentItemBindings {
  @JvmStatic
  @BindingAdapter(value = ["currentItemAttrChanged"])
  fun setListeners(viewPager: ViewPager, inverseBindingListener: InverseBindingListener) {
    val newListener = object : ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) =
          inverseBindingListener.onChange()
    }

    val oldListener = ListenerUtil.trackListener<ViewPager.OnPageChangeListener>(
        viewPager,
        newListener,
        R.id.onPageChangeListener
    )

    if (oldListener != null) {
      viewPager.removeOnPageChangeListener(oldListener)
    }
    viewPager.addOnPageChangeListener(newListener)
  }

  @JvmStatic
  @BindingAdapter("currentItem", "tripGroups", requireAll = true)
  fun setCurrentItem(view: ViewPager, currentItem: Int, tripGroups: List<TripGroup>) {
    check(view.adapter != null) { "PagerAdapter must be specified first" }

    (view.adapter as TripGroupsPagerAdapter).takeIf {
      tripGroups.map { it.uuid() } != it.tripGroups?.map { it.uuid() }
    }?.tripGroups = tripGroups

    if (currentItem != view.currentItem) {
      view.setCurrentItem(currentItem, false)
    }
  }
}
