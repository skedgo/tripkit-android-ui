package com.skedgo.tripkit.ui.generic.bottomsheet

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import timber.log.Timber
import java.util.Stack

data class CardSettings(
    val fixedHeight: Boolean,
    @BottomSheetBehavior.State val startingState: Int,
    val peekHeight: Int = 120,
    val expandedOffset: Int = 0,
)

interface CardableFragment {
    fun defaultCardSettings(): CardSettings
}

/*
 * The CardManager keeps track of card sizes, and can change the size of the card (for example, expanding or setting to
 * half-height).
 */
class BottomSheetCardsManager(
    private val bottomSheetView: View,
    private val bottomSheet: FrameLayout
) {
    private var behavior: BottomSheetBehavior<View> = BottomSheetBehavior.from(bottomSheetView)
    private var stack: Stack<CardSettings> = Stack()

    /**
     * Sets up a card's default settings.
     */
    fun setupFragment(cardableFragment: CardableFragment, forceCardState: Int? = null) {
        val settings = cardableFragment.defaultCardSettings()
        behavior.isFitToContents = settings.fixedHeight
        behavior.state = forceCardState ?: settings.startingState
        behavior.isDraggable = !settings.fixedHeight
        behavior.halfExpandedRatio = 0.5f
        bottomSheet.let { frameLayout ->
            frameLayout.layoutParams = frameLayout.layoutParams.apply {
                height =
                    if (settings.fixedHeight) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

    }

    fun push() {
        behavior.let {
            stack.push(
                CardSettings(
                    it.isFitToContents,
                    it.state,
                    it.peekHeight,
                    it.expandedOffset
                )
            )
        }
    }

    /**
     * Restore the last card settings. This should be called *after* the fragment backstack has been popped.
     */
    fun restore() {
        if (!stack.empty()) {
            val settings = stack.pop()
            behavior.isFitToContents = settings.fixedHeight
            if (settings.startingState != BottomSheetBehavior.STATE_SETTLING
                && settings.startingState != BottomSheetBehavior.STATE_DRAGGING) {
                behavior.state = settings.startingState
            } else {
                Timber.e("Invalid state: ${settings.startingState}")
            }
        }

    }

    fun setState(state: Int) {
        behavior.state = state
    }

    fun setExpandOffset(offset: Int) {
        behavior.expandedOffset = offset
    }

    fun getState(): Int = behavior.state

    fun getBehavior(): BottomSheetBehavior<View> = behavior
}