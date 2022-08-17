package com.skedgo.tripkit.ui.trippreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.booking.quickbooking.Ticket
import com.skedgo.tripkit.ui.databinding.FragmentTripPreviewFooterBinding
import com.skedgo.tripkit.ui.trippreview.drt.DrtTicketViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject

class TripPreviewTicketFooterFragment : Fragment() {

    private val viewModel: TripPreviewTicketViewModel by viewModels()

    lateinit var binding: FragmentTripPreviewFooterBinding

    private val disposeBag = CompositeDisposable()
    private var selectedTickets: PublishSubject<List<DrtTicketViewModel>>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTripPreviewFooterBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObserver()
    }

    private fun initObserver() {
        selectedTickets?.subscribeOn(AndroidSchedulers.mainThread())
            ?.subscribe { vms ->
                var totalTickets = 0.0
                var numberTickets = 0L

                val updatedTickets = ArrayList<Ticket>()
                vms.forEach {
                    totalTickets += (it.price.value ?: 0.0).times(it.value.value ?: 0)
                    numberTickets += (it.value.value ?: 0)
                    it.ticket.value?.let { ticket ->
                        updatedTickets.add(ticket)
                    }
                }
                viewModel.setTickets(updatedTickets)
                viewModel.setTotalTickets(totalTickets, (vms.first().currency.value ?: ""))
                viewModel.setNumberTickets(numberTickets)
                viewModel.setShowView(!(totalTickets == 0.0 && numberTickets == 0L))
            }?.addTo(disposeBag)
    }

    companion object {

        const val TAG = "TripPreviewFooter"

        fun newInstance(
            selectedTickets: PublishSubject<List<DrtTicketViewModel>>?
        ): TripPreviewTicketFooterFragment {
            return TripPreviewTicketFooterFragment().apply {
                this.selectedTickets = selectedTickets
            }
        }
    }
}