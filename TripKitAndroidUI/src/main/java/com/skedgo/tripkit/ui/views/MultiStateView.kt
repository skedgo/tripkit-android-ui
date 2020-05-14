package com.skedgo.tripkit.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.Nullable
import timber.log.Timber

/*
    Adapted from https://github.com/Kennyc1012/MultiStateView/blob/master/library/src/main/java/com/kennyc/view/MultiStateView.kt
 */

class MultiStateView(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {

    }
    enum class ViewState {
        LOADING,
        ERROR,
        EMPTY,
        CONTENT
    }

    private var contentView: View? = null
    private var loadingView: View? = null
    private var errorView: View? = null
    private var emptyView: View? = null

    var listener: StateListener? = null

    var animateLayoutChanges: Boolean = false

    var viewState: ViewState = ViewState.CONTENT
        set(value) {
            val previousField = field

            if (value != previousField) {
                field = value
                setView(previousField)
                listener?.onStateChanged(value)
            }
        }


    @Nullable
    fun getView(state: ViewState): View? {
        return when (state) {
            ViewState.LOADING -> loadingView
            ViewState.CONTENT -> contentView
            ViewState.EMPTY -> emptyView
            ViewState.ERROR -> errorView
        }
    }

    /**
     * Sets the view for the given view state
     *
     * @param view          The [View] to use
     * @param state         The [com.skedgo.tripkit.ui.views.MultiStateView.ViewState]to set
     * @param switchToState If the [com.skedgo.tripkit.ui.views.MultiStateView.ViewState] should be switched to
     */
    fun setViewForState(view: View, state: ViewState, switchToState: Boolean = false) {
        when (state) {
            ViewState.LOADING -> {
                if (view == loadingView) return
                if (loadingView != null) removeView(loadingView)
                loadingView = view
                addView(view)
            }

            ViewState.EMPTY -> {
                if (view == emptyView) return
                if (emptyView != null) removeView(emptyView)
                emptyView = view
                addView(view)
            }

            ViewState.ERROR -> {
                if (view == errorView) return
                if (errorView != null) removeView(errorView)
                errorView = view
                addView(view)
            }

            ViewState.CONTENT -> {
                if (view == contentView) return
                if (contentView != null) removeView(contentView)
                contentView = view
                addView(view)
            }
        }

        if (switchToState) viewState = state
    }

    fun setViewForState(layoutRes: Int, state: ViewState, switchToState: Boolean = false) {
        if (layoutRes <= 0) return
        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        setViewForState(view, state, switchToState)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (contentView == null) throw IllegalArgumentException("Content view is not defined")

        when (viewState) {
            ViewState.CONTENT -> setView(ViewState.CONTENT)
            else -> contentView?.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        return when (val superState = super.onSaveInstanceState()) {
            null -> superState
            else -> SavedState(superState, viewState)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            viewState = state.state
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun addView(child: View) {
        if (isValidContentView(child)) contentView = child
        super.addView(child)
    }

    override fun addView(child: View, index: Int) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, index)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, index, params)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, params)
    }

    override fun addView(child: View, width: Int, height: Int) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, width, height)
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams): Boolean {
        if (isValidContentView(child)) contentView = child
        return super.addViewInLayout(child, index, params)
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams, preventRequestLayout: Boolean): Boolean {
        if (isValidContentView(child)) contentView = child
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    private fun isValidContentView(view: View): Boolean {
        return if (contentView != null && contentView !== view) {
            false
        } else view != loadingView && view != errorView && view != emptyView
    }

    fun hideView(view: View?) {
        view?.visibility = View.GONE
    }

    private fun setView(previousState: ViewState) {
        when (viewState) {
            ViewState.LOADING -> {
                loadingView?.let {
                    hideView(contentView)
                    hideView(errorView)
                    hideView(emptyView)

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        it.visibility = View.VISIBLE
                    }
                }
            }

            ViewState.EMPTY -> {
                emptyView?.let {
                    hideView(contentView)
                    hideView(errorView)
                    hideView(loadingView)

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        it.visibility = View.VISIBLE
                    }
                }
            }

            ViewState.ERROR -> {
                errorView?.let {
                    hideView(contentView)
                    hideView(emptyView)
                    hideView(loadingView)

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        it.visibility = View.VISIBLE
                    }
                }
            }

            ViewState.CONTENT -> {
                contentView?.let {
                    hideView(loadingView)
                    hideView(errorView)
                    hideView(emptyView)

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        it.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    private fun animateLayoutChange(@Nullable previousView: View?) {
        if (previousView == null) {
            requireNotNull(getView(viewState)).visibility = View.VISIBLE
            return
        }

        ObjectAnimator.ofFloat(previousView, "alpha", 1.0f, 0.0f).apply {
            duration = 250L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    previousView.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    previousView.visibility = View.GONE
                    val currentView = requireNotNull(getView(viewState))
                    currentView.visibility = View.VISIBLE
                    ObjectAnimator.ofFloat(currentView, "alpha", 0.0f, 1.0f).setDuration(250L).start()
                }
            })
        }.start()
    }

    interface StateListener {
        fun onStateChanged(viewState: ViewState)
    }

    private class SavedState : BaseSavedState {
        internal val state: ViewState

        constructor(superState: Parcelable, state: ViewState) : super(superState) {
            this.state = state
        }

        constructor(parcel: Parcel) : super(parcel) {
            state = parcel.readSerializable() as ViewState
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeSerializable(state)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}