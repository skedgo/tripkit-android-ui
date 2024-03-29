package com.skedgo.tripkit.ui.utils

import androidx.fragment.app.Fragment

//For generic fragment replace use
fun Fragment.replaceFragment(fragment: Fragment, tag: String, containerViewId: Int, checkBackStack: Boolean = true) {

    if (checkBackStack) {
        if (childFragmentManager.findFragmentByTag(tag)?.isVisible == true) {
            return
        }
    }

    childFragmentManager
            .beginTransaction()
            .replace(containerViewId, fragment, tag)
            .addToBackStack(tag)
            .commit()
}