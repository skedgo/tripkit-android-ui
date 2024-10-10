package com.skedgo.tripkit.ui.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

//For generic fragment replace use
fun Fragment.replaceFragment(
    fragment: Fragment,
    tag: String,
    containerViewId: Int,
    checkBackStack: Boolean = true
) {

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

fun Fragment.replaceFragment(
    fragment: Fragment,
    tag: String,
    containerViewId: Int,
    checkBackStack: Boolean = true,
    addToBackStack: Boolean = true
) {

    if (checkBackStack) {
        if (childFragmentManager.findFragmentByTag(tag)?.isVisible == true) {
            return
        }
    }

    val transaction = childFragmentManager
        .beginTransaction()
        .replace(containerViewId, fragment, tag)
    if (addToBackStack) {
        transaction.addToBackStack(tag)
    }
    transaction.commit()
}

fun Fragment.popSpecificWithTag(tag: String) {
    childFragmentManager.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
}