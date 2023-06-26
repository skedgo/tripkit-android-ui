package com.skedgo.tripkit.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.skedgo.tripkit.ui.R

/**
 * Was deprecated after setting min SDK 15.
 */
// TODO - Remove this class
@Deprecated("")
class FlatAlertDialogBuilder(private val mContext: Context) {
    private var mPositiveTextResourceId = -1
    private var mNegativeTextResourceId = -1
    private var mContentViewResourceId = -1
    private var mTitle: String? = null
    private var mOnPositiveClickListener: DialogInterface.OnClickListener? = null
    private var mOnNegativeClickListener: DialogInterface.OnClickListener? = null
    private var mNegativeActionAccessibilityDelegate: View.AccessibilityDelegate? = null
    lateinit var contentView: View

    fun setTitle(titleTextResourceId: Int): FlatAlertDialogBuilder {
        mTitle = mContext.getString(titleTextResourceId)
        return this
    }

    fun setTitle(title: String?): FlatAlertDialogBuilder {
        mTitle = title
        return this
    }

    fun setPositiveButton(
        positiveTextResourceId: Int,
        onPositiveClickListener: DialogInterface.OnClickListener?
    ): FlatAlertDialogBuilder {
        mPositiveTextResourceId = positiveTextResourceId
        mOnPositiveClickListener = onPositiveClickListener
        return this
    }

    fun setNegativeButton(
        negativeTextResourceId: Int,
        onNegativeClickListener: DialogInterface.OnClickListener?,
        accessibilityDelegate: View.AccessibilityDelegate? = null
    ): FlatAlertDialogBuilder {
        mNegativeTextResourceId = negativeTextResourceId
        mOnNegativeClickListener = onNegativeClickListener
        mNegativeActionAccessibilityDelegate = accessibilityDelegate
        return this
    }

    fun setContentView(contentViewResourceId: Int): FlatAlertDialogBuilder {
        mContentViewResourceId = contentViewResourceId
        return this
    }

    fun create(): Dialog {
        return createDialog()
    }

    private fun createDialog(): Dialog {
        val dialog =
            Dialog(mContext, R.style.FlatDialog)
        val dialogView = dialog.layoutInflater
            .inflate(R.layout.v4_flat_dialog_part_container, null)
        if (dialogView != null) {
            val titleTextView =
                dialogView.findViewById<View>(R.id.v4_dialog_title) as TextView
            titleTextView.text = mTitle
            val buttonBar =
                dialogView.findViewById<View>(R.id.toolbar)
            configureButtonBar(dialog, buttonBar)
            val contentLayout =
                dialogView.findViewById<View>(R.id.v4_dialog_content) as FrameLayout
            contentView =
                dialog.layoutInflater.inflate(mContentViewResourceId, contentLayout, false)
            if (contentView != null) {
                contentLayout.addView(contentView)
            }
        }
        dialog.setContentView(
            dialogView!!,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return dialog
    }

    private fun configureButtonBar(
        dialog: Dialog,
        buttonBar: View
    ) {
        val negativeButton =
            buttonBar.findViewById<View>(R.id.v4_btn_negative) as Button
        configureNegativeButton(dialog, negativeButton)
        val positiveButton =
            buttonBar.findViewById<View>(R.id.v4_btn_positive) as Button
        configurePositiveButton(dialog, positiveButton)

        // Hides the button bar if there is no buttons visible anymore
        if (negativeButton.visibility == View.GONE && positiveButton.visibility == View.GONE) {
            buttonBar.visibility = View.GONE
        }
    }

    private fun configurePositiveButton(
        dialog: Dialog,
        positiveButton: Button
    ) {
        if (mPositiveTextResourceId != -1) {
            positiveButton.setText(mPositiveTextResourceId)
            positiveButton.setOnClickListener {
                mOnPositiveClickListener!!.onClick(
                    dialog,
                    DialogInterface.BUTTON_POSITIVE
                )
            }
        } else {
            positiveButton.visibility = View.GONE
        }
    }

    private fun configureNegativeButton(
        dialog: Dialog,
        negativeButton: Button
    ) {
        if (mNegativeTextResourceId != -1) {
            negativeButton.setText(mNegativeTextResourceId)
            negativeButton.setOnClickListener {
                mOnNegativeClickListener!!.onClick(
                    dialog,
                    DialogInterface.BUTTON_NEGATIVE
                )
            }
            mNegativeActionAccessibilityDelegate?.let {
                negativeButton.accessibilityDelegate = it
            }
        } else {
            negativeButton.visibility = View.GONE
        }
    }

}