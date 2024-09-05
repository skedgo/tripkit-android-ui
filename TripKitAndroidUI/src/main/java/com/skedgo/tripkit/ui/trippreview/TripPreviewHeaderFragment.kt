package com.skedgo.tripkit.ui.trippreview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.databinding.FragmentTripPreviewHeaderBinding
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummary
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentsSummaryData
import com.skedgo.tripkit.ui.utils.observe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TripPreviewHeaderFragment : Fragment() {

    private val viewModel: TripPreviewHeaderViewModel by viewModels()

    lateinit var binding: FragmentTripPreviewHeaderBinding

    private val disposeBag = CompositeDisposable()
    private var pageIndexStream: PublishSubject<Pair<Long, String>>? = null
    private var hideExactTimes: Boolean = false
    private var loadQuickBookingCallback: (TripSegment?) -> Unit = { _ -> }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTripPreviewHeaderBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObserver()
        initViews()
    }

    private fun initViews() {
        lifecycleScope.launch {
            delay(500)
            checkLoadedHeaders()
        }
        binding.bAction.setOnClickListener {
            loadQuickBookingCallback.invoke(viewModel.quickBookingSegment.value)
        }
    }

    private fun initObserver() {
        pageIndexStream?.subscribeOn(AndroidSchedulers.mainThread())
            ?.subscribe {
                viewModel.setSelectedById(it.first, it.second)
                checkSelectedItemOnLoadedHeaders(binding.rvHeaders.layoutManager as LinearLayoutManager)
            }?.addTo(disposeBag)

        viewModel.apply {
            observe(selectedSegmentId) {
                it?.let { pageIndexStream?.onNext(it) }
            }
            setHideExactTimes(this@TripPreviewHeaderFragment.hideExactTimes)
        }
    }

    fun setHeaderItems(data: TripSegmentsSummaryData) {
        if (!this@TripPreviewHeaderFragment.isDetached) {
            viewModel.setup(requireContext(), data)
        }
    }

    private fun checkLoadedHeaders() {
        val layoutManager = binding.rvHeaders.layoutManager as LinearLayoutManager
        val adapter = binding.rvHeaders.adapter
        binding.rvHeaders.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Get the total number of items in the adapter
                val totalItemCount = adapter?.itemCount ?: 0

                // Get the last visible item position
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                // Check if the RecyclerView has scrolled to the last item
                if (lastVisibleItemPosition >= totalItemCount - 1) {
                    // RecyclerView has loaded all items
                    // You can trigger a callback or any other logic here
                    checkSelectedItemOnLoadedHeaders(layoutManager)
                    binding.rvHeaders.removeOnScrollListener(this)
                }
            }
        })
    }

    private fun checkSelectedItemOnLoadedHeaders(
        layoutManager: LinearLayoutManager
    ) {
        // Find the position of the item where selected = true
        val selectedItemPosition = viewModel.items.indexOfFirst { it.selected.get() }
        // Check if the item with the selected flag is visible
        if (selectedItemPosition != -1) {
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

            // If the item is not in the visible range, scroll to it
            if (selectedItemPosition < firstVisiblePosition || selectedItemPosition > lastVisiblePosition) {
                binding.rvHeaders.scrollToPosition(selectedItemPosition)
            }
        }
    }

    companion object {

        const val TAG = "TripPreviewHeader"

        fun newInstance(
            pageIndexStream: PublishSubject<Pair<Long, String>>?,
            hideExactTimes: Boolean,
            loadQuickBookingCallback: (TripSegment?) -> Unit = { _ -> }
        ): TripPreviewHeaderFragment {
            return TripPreviewHeaderFragment().apply {
                this.pageIndexStream = pageIndexStream
                this.hideExactTimes = hideExactTimes
                this.loadQuickBookingCallback = loadQuickBookingCallback
            }
        }
    }
}