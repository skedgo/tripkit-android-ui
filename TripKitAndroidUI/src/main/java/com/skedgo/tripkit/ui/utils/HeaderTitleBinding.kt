package com.skedgo.tripkit.ui.utils

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.view.ViewTreeObserver
import com.skedgo.tripkit.ui.R
import androidx.databinding.BindingAdapter

/** Reserves symmetric action space, moving a long title below the actions when words cannot fit. */
@BindingAdapter("headerStartAction", "headerEndAction", requireAll = false)
fun bindHeaderTitleActions(title: TextView, startAction: View?, endAction: View?) {
    val parent = title.parent as? ConstraintLayout ?: return
    fun update() {
        val available = parent.width - parent.paddingLeft - parent.paddingRight
        if (available <= 0) return
        val startWidth = startAction?.takeIf { it.visibility != View.GONE }?.width ?: 0
        val endWidth = endAction?.takeIf { it.visibility != View.GONE }?.width ?: 0
        val reserved = maxOf(startWidth, endWidth)
        val longestWord = title.text.toString().split(Regex("\\s+"))
            .maxOfOrNull { title.paint.measureText(it) } ?: 0f
        val below = longestWord + title.paddingLeft + title.paddingRight > available - 2 * reserved
        val params = title.layoutParams as ConstraintLayout.LayoutParams
        val actions = listOfNotNull(startAction, endAction).filter { it.visibility != View.GONE }
        val topTarget = if (below) actions.maxBy { it.height }.id else ConstraintSet.PARENT_ID
        val margin = if (below) 0 else reserved
        if (params.width != 0 || params.marginStart != margin || params.marginEnd != margin ||
            params.topToBottom != (if (below) topTarget else ConstraintSet.UNSET)) {
            params.width = 0
            params.startToStart = ConstraintSet.PARENT_ID
            params.startToEnd = ConstraintSet.UNSET
            params.endToEnd = ConstraintSet.PARENT_ID
            params.marginStart = margin
            params.marginEnd = margin
            params.topToTop = if (below) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
            params.topToBottom = if (below) topTarget else ConstraintSet.UNSET
            params.bottomToBottom = if (below) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
            title.layoutParams = params
        }
        for (action in actions) {
            val actionParams = action.layoutParams as ConstraintLayout.LayoutParams
            val bottom = if (below) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
            if (actionParams.bottomToBottom != bottom) {
                actionParams.bottomToBottom = bottom
                action.layoutParams = actionParams
            }
        }
    }
    (title.getTag(R.id.header_title_layout_listener) as? ViewTreeObserver.OnGlobalLayoutListener)?.let {
        parent.viewTreeObserver.removeOnGlobalLayoutListener(it)
    }
    val listener = ViewTreeObserver.OnGlobalLayoutListener { update() }
    title.setTag(R.id.header_title_layout_listener, listener)
    parent.viewTreeObserver.addOnGlobalLayoutListener(listener)
}
