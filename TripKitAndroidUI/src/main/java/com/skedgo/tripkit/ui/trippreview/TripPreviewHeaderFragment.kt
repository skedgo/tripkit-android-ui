package com.skedgo.tripkit.ui.trippreview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.ui.databinding.FragmentTripPreviewHeaderBinding
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummary
import com.skedgo.tripkit.ui.utils.observe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject


class TripPreviewHeaderFragment : Fragment() {

    private val viewModel: TripPreviewHeaderViewModel by viewModels()

    lateinit var binding: FragmentTripPreviewHeaderBinding

    private val disposeBag = CompositeDisposable()
    private var pageIndexStream: PublishSubject<Pair<Long, String>>? = null
    private var hideExactTimes: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentTripPreviewHeaderBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObserver()
    }

    private fun initObserver() {
        pageIndexStream?.subscribeOn(AndroidSchedulers.mainThread())
                ?.subscribe {
                    viewModel.setSelectedById(it.first, it.second)
                }?.addTo(disposeBag)

        viewModel.apply {
            observe(selectedSegmentId) {
                it?.let { pageIndexStream?.onNext(it) }
            }
            setHideExactTimes(this@TripPreviewHeaderFragment.hideExactTimes)
        }
    }

    fun setHeaderItems(items: List<TripSegmentSummary>) {
        if (!this@TripPreviewHeaderFragment.isDetached) {
            viewModel.setup(requireContext(), items)
        }
    }

    companion object {

        const val TAG = "TripPreviewHeader"

        fun newInstance(
                pageIndexStream: PublishSubject<Pair<Long, String>>?,
                hideExactTimes: Boolean
        ): TripPreviewHeaderFragment {
            return TripPreviewHeaderFragment().apply {
                this.pageIndexStream = pageIndexStream
                this.hideExactTimes = hideExactTimes
            }
        }
    }
}