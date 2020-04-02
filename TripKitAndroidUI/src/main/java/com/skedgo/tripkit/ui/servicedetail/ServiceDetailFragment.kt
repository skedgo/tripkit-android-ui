package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.model.ServiceStop
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.ServiceDetailFragmentBinding
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.ARG_TIMETABLE_ENTRY
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class ServiceDetailFragment : BaseTripKitFragment() {
    interface OnScheduledStopClickListener {
        fun onScheduledStopClicked(stop: ServiceStop)
    }
    private var clickListener: MutableList<OnScheduledStopClickListener> = mutableListOf()
    fun addOnScheduledStopClickListener(callback: OnScheduledStopClickListener) {
        if (!clickListener.contains(callback)) {
            clickListener.add(callback)
        }
    }

    fun addOnScheduledStopClickListener(listener:(ServiceStop) -> Unit) {
        this.clickListener.add(object: OnScheduledStopClickListener {
            override fun onScheduledStopClicked(stop: ServiceStop) {
                listener(stop)
            }
        })
    }

    @Inject
    lateinit var viewModel: ServiceDetailViewModel
    lateinit var binding: ServiceDetailFragmentBinding
    private var stop: ScheduledStop? = null
    private var timetableEntry: TimetableEntry? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().inject(this);
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onItemClicked
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { stop ->
                    this.clickListener.forEach{
                        it.onScheduledStopClicked(stop)
                    }
                }.subscribe().addTo(autoDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = ServiceDetailFragmentBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.occupancyList.isNestedScrollingEnabled = false
        binding.recyclerView.isNestedScrollingEnabled = true
        binding.occupancyList.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stop = arguments?.getParcelable(ARG_STOP)
        timetableEntry = arguments?.getParcelable(ARG_TIMETABLE_ENTRY)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (stop != null && timetableEntry != null) {
            viewModel.setup(stop!!, timetableEntry!!)
        }
    }
        class Builder {
        private var stop: ScheduledStop? = null
        private var timetableEntry: TimetableEntry? = null

        fun withTimetableEntry(timetableEntry: TimetableEntry?): Builder {
            this.timetableEntry = timetableEntry
            return this
        }

        fun withStop(stop: ScheduledStop?): Builder {
            this.stop = stop
            return this
        }

        fun build(): ServiceDetailFragment {
            val args = Bundle()
            val fragment = ServiceDetailFragment()
            args.putParcelable(ARG_STOP, stop)
            args.putParcelable(ARG_TIMETABLE_ENTRY, timetableEntry)
            fragment.arguments = args
            return fragment
        }
    }

}
