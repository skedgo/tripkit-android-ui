package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.databinding.FragmentTripPreviewFooterBinding
import com.skedgo.tripkit.ui.interactor.TripKitEvent
import com.skedgo.tripkit.ui.interactor.TripKitEventBus
import com.skedgo.tripkit.ui.payment.PaymentData
import com.skedgo.tripkit.ui.utils.observe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class TripPreviewTicketFooterFragment : Fragment() {

    private val viewModel: TripPreviewTicketViewModel by viewModels()

    @Inject
    lateinit var tripKitEventBus: TripKitEventBus

    lateinit var binding: FragmentTripPreviewFooterBinding

    private val disposeBag = CompositeDisposable()
    private var paymentData: PublishSubject<PaymentData>? = null
    private var onViewSummaryCallback: ((PaymentData) -> Unit)? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

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
        /*
        drtItems?.subscribeOn(AndroidSchedulers.mainThread())
                ?.subscribe { vms ->
                    var totalTickets = 0.0
                    var numberTickets = 0L

                    val updatedTickets = ArrayList<Ticket>()
                    vms.second.forEach {
                        totalTickets += (it.price.value ?: 0.0).times(it.value.value ?: 0)
                        numberTickets += (it.value.value ?: 0)
                        it.ticket.value?.let { ticket ->
                            updatedTickets.add(ticket)
                        }
                    }
                    viewModel.setTickets(updatedTickets)
                    viewModel.setTotalTickets(totalTickets, (vms.second.first().currency.value
                            ?: ""))
                    viewModel.setNumberTickets(numberTickets)
                    viewModel.setShowView(!(totalTickets == 0.0 && numberTickets == 0L))

                    viewModel.setDrtItems(vms.first)
                    viewModel.setDrtTickets(vms.second)
                }?.addTo(disposeBag)
        */

        paymentData?.subscribeOn(AndroidSchedulers.mainThread())
                ?.subscribe { paymentData ->
                    viewModel.setPaymentData(paymentData)
                    compute(paymentData)
                }?.addTo(disposeBag)

        observe(viewModel.paymentData) {
            it?.let { paymentData ->
                compute(paymentData)
            }
        }

        observe(viewModel.goToPayment) {
            it?.let {
                viewModel.paymentData.value?.let {
                    tripKitEventBus.publish(
                            TripKitEvent.OnBookDrt(
                                    it.drtFragmentHashCode
                            ) { success, paymentData ->
                                if (success && paymentData != null) {
                                    viewModel.setPaymentData(paymentData)
                                    //startActivity(PaymentActivity.getIntent(requireActivity(), paymentData))
                                    onViewSummaryCallback?.invoke(paymentData)
                                }
                            }
                    )
                }

            }
        }

        TripKitEventBus.listen(TripKitEvent.OnToggleDrtFooterVisibility::class.java).subscribe {
            viewModel.setShowView(!it.isHide)
        }.addTo(disposeBag)
    }

    private fun compute(paymentData: PaymentData) {
        val totalPrice = if (paymentData.paymentSummaryDetails.isNotEmpty()) paymentData.paymentSummaryDetails.sumOf {
            it.getConvertedPrice() * (it.breakdown?.toDouble() ?: 0.0)
        } else 0.0
        val numberTickets = (if (paymentData.paymentSummaryDetails.isNotEmpty()) paymentData.paymentSummaryDetails.sumOf {
            it.breakdown ?: 0
        } else 0.0).toInt()

        val currency = paymentData.paymentSummaryDetails.firstOrNull { it.price != null }?.currency

        viewModel.setTotalTickets(totalPrice, currency ?: "")
        viewModel.setNumberTickets(numberTickets)
        viewModel.setShowView(!(totalPrice == 0.0 && numberTickets == 0))
    }

    companion object {

        const val TAG = "TripPreviewFooter"

        /*
        fun newInstance(
                drtItems: PublishSubject<Pair<List<DrtItemViewModel>, List<DrtTicketViewModel>>>?
        ): TripPreviewTicketFooterFragment {
            return TripPreviewTicketFooterFragment().apply {
                this.drtItems = drtItems
            }
        }
        */

        fun newInstance(
                paymentData: PublishSubject<PaymentData>?,
                onViewSummaryCallback: (PaymentData) -> Unit
        ): TripPreviewTicketFooterFragment {
            return TripPreviewTicketFooterFragment().apply {
                this.paymentData = paymentData
                this.onViewSummaryCallback = onViewSummaryCallback
            }
        }
    }
}