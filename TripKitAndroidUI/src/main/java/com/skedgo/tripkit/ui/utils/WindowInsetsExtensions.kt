package com.skedgo.tripkit.ui.utils

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.skedgo.tripkit.ui.R

private const val EDGE_TO_EDGE_ENFORCEMENT_API_LEVEL = 35

private data class OriginalMargins(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

private data class OriginalPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * Keeps top overlays inside the safe drawing area when Android enforces edge-to-edge.
 * Existing margins remain the design-space baseline for every inset dispatch.
 */
fun View.applyEdgeToEdgeSafeAreaMargins(
    applyLeft: Boolean = false,
    applyTop: Boolean = false,
    applyRight: Boolean = false,
    applyBottom: Boolean = false,
) {
    if (Build.VERSION.SDK_INT < EDGE_TO_EDGE_ENFORCEMENT_API_LEVEL) return

    val marginLayoutParams = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val originalMargins =
        (getTag(R.id.edge_to_edge_original_margins) as? OriginalMargins)
            ?: OriginalMargins(
                left = marginLayoutParams.leftMargin,
                top = marginLayoutParams.topMargin,
                right = marginLayoutParams.rightMargin,
                bottom = marginLayoutParams.bottomMargin,
            ).also { setTag(R.id.edge_to_edge_original_margins, it) }

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val safeArea = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            ?: return@setOnApplyWindowInsetsListener windowInsets

        val left = originalMargins.left + if (applyLeft) safeArea.left else 0
        val top = originalMargins.top + if (applyTop) safeArea.top else 0
        val right = originalMargins.right + if (applyRight) safeArea.right else 0
        val bottom = originalMargins.bottom + if (applyBottom) safeArea.bottom else 0

        if (params.leftMargin != left || params.topMargin != top ||
            params.rightMargin != right || params.bottomMargin != bottom
        ) {
            params.setMargins(left, top, right, bottom)
            view.layoutParams = params
        }
        windowInsets
    }

    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}

/**
 * Adds the safe drawing area to a container's existing padding when Android enforces edge-to-edge.
 * This lets the container background continue behind the system bars while keeping its content safe.
 */
fun View.applyEdgeToEdgeSafeAreaPadding(
    applyLeft: Boolean = false,
    applyTop: Boolean = false,
    applyRight: Boolean = false,
    applyBottom: Boolean = false,
) {
    if (Build.VERSION.SDK_INT < EDGE_TO_EDGE_ENFORCEMENT_API_LEVEL) return

    val originalPadding =
        (getTag(R.id.edge_to_edge_original_padding) as? OriginalPadding)
            ?: OriginalPadding(
                left = paddingLeft,
                top = paddingTop,
                right = paddingRight,
                bottom = paddingBottom,
            ).also { setTag(R.id.edge_to_edge_original_padding, it) }

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val safeArea = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val left = originalPadding.left + if (applyLeft) safeArea.left else 0
        val top = originalPadding.top + if (applyTop) safeArea.top else 0
        val right = originalPadding.right + if (applyRight) safeArea.right else 0
        val bottom = originalPadding.bottom + if (applyBottom) safeArea.bottom else 0

        if (view.paddingLeft != left || view.paddingTop != top ||
            view.paddingRight != right || view.paddingBottom != bottom
        ) {
            view.setPadding(left, top, right, bottom)
        }
        windowInsets
    }

    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}
