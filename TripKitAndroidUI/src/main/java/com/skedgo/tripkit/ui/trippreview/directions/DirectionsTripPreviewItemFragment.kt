package com.skedgo.tripkit.ui.trippreview.directions

import android.content.Intent
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerDirectionsItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class DirectionsTripPreviewItemFragment : BaseFragment<TripPreviewPagerDirectionsItemBinding>() {

    private val viewModel: DirectionsTripPreviewItemViewModel by viewModels()

    private var segment: TripSegment? = null

    override val layoutRes: Int
        get() = R.layout.trip_preview_pager_directions_item

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        segment?.let {
            viewModel.setSegment(requireContext(), it)
        } ?: kotlin.run {
            savedInstanceState?.let {
                if (it.containsKey(ARGS_SEGMENT)) {
                    segment = Gson().fromJson(it.getString(ARGS_SEGMENT), TripSegment::class.java)
                    segment?.let { viewModel.setSegment(requireContext(), it) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        viewModel.showLaunchInMapsClicked.observable.onEach {
            it.segment?.let {
                /*
                val mode = if (it.isCycling) {
                    "b"
                } else if (it.isWalking || it.isWheelchair) {
                    "w"
                } else {
                    "d"
                }
                val uri = Uri.parse("google.navigation:mode=$mode&q=${it.to.lat},${it.to.lon}")
                */
                val mode = if (it.isCycling) {
                    "bicycling"
                } else if (it.isWalking || it.isWheelchair) {
                    "walking"
                } else {
                    "driving"
                }
                val uri =
                    Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${it.from?.lat},${it.from?.lon}&destination=${it.to?.lat},${it.to?.lon}&travelmode=$mode")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                //mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                }
            }
        }.launchIn(lifecycleScope)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putString(ARGS_SEGMENT, Gson().toJson(segment))
    }

    override fun onCreated(savedInstance: Bundle?) {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.rvDirections.addItemDecoration(LastItemExcludedDividerDecoration(requireContext()))
    }

    private class LastItemExcludedDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val divider: Drawable? =
            context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider)).let { ta ->
                ta.getDrawable(0).also { ta.recycle() }
            }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val divider = divider ?: return
            val itemCount = parent.adapter?.itemCount ?: 0
            if (itemCount <= 1) return

            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION || position >= itemCount - 1) continue

                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin + child.translationY.toInt()
                val bottom = top + divider.intrinsicHeight
                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val itemCount = parent.adapter?.itemCount ?: 0
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION || position >= itemCount - 1) {
                outRect.set(0, 0, 0, 0)
            } else {
                outRect.set(0, 0, 0, divider?.intrinsicHeight ?: 0)
            }
        }
    }

    companion object {

        const val ARGS_SEGMENT = "args_segment"

        fun newInstance(segment: TripSegment): DirectionsTripPreviewItemFragment {
            val fragment = DirectionsTripPreviewItemFragment()
            fragment.segment = segment
            return fragment
        }
    }
}