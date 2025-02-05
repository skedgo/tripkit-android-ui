package com.skedgo.tripkit.ui.map

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.ViewUtils

/**
 * View to create custom callout in [InfoWindowAdapter],
 * simply including a title, a snippet, a left image and a right image.
 */
class SimpleCalloutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var titleView: TextView
    private lateinit var snippetView: TextView
    private lateinit var rightImageView: ImageView
    private lateinit var leftImageView: ImageView

    companion object {
        fun create(inflater: LayoutInflater): SimpleCalloutView {
            return inflater.inflate(R.layout.view_simple_callout, null) as SimpleCalloutView
        }
    }

    fun setTitle(title: String?) {
        ViewUtils.setText(titleView, title)
    }

    fun setSnippet(snippet: String?) {
        ViewUtils.setText(snippetView, snippet)
    }

    fun setLeftImage(@DrawableRes res: Int) {
        ViewUtils.setImage(leftImageView, res)
    }

    fun setRightImage(@DrawableRes res: Int) {
        ViewUtils.setImage(rightImageView, res)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        titleView = findViewById(R.id.titleView)
        snippetView = findViewById(R.id.snippetView)
        rightImageView = findViewById(R.id.rightImageView)
        leftImageView = findViewById(R.id.leftImageView)
    }
}
