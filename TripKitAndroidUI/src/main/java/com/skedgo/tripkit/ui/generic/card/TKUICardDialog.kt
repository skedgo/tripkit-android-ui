package com.skedgo.tripkit.ui.generic.card

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment

class TKUICardDialog(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val fragmentClass: Class<out Fragment>,
    private val fragmentArgs: Bundle? = null,
    private val mapFragment: TripKitMapFragment? = null,
    private val cardManager: TKUICardManager? = null
) {
    private var dialog: BottomSheetDialog? = null

    fun show() {
        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_tkui_card_container, null, false)
        val container = contentView.findViewById<FrameLayout>(R.id.contentFrame)

        dialog = BottomSheetDialog(context, R.style.Theme_MaterialComponents_BottomSheetDialog).apply {
            setContentView(contentView)

            setOnShowListener {
                val bottomSheet = findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

                val behavior = BottomSheetBehavior.from(bottomSheet!!)
                behavior.isFitToContents = false
                behavior.halfExpandedRatio = 0.5f
                behavior.expandedOffset = 0
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isHideable = false

                // ✅ Delay fragment commit until container is attached
                Handler(Looper.getMainLooper()).postDelayed({
                    val fragment = fragmentClass.newInstance().apply {
                        arguments = fragmentArgs
                    }

                    if (fragment is TKUICardBaseFragment<*>) {
                        fragment.mapFragment = mapFragment
                        fragment.cardManager = cardManager
                    }

                    fragmentManager.beginTransaction()
                        .replace(container.id, fragment)
                        .commitAllowingStateLoss()
                }, 1000)
            }
        }

        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}

