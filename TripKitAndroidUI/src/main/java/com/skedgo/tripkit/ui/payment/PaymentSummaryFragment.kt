package com.skedgo.tripkit.ui.payment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentPaymentSummaryBinding
import com.skedgo.tripkit.ui.utils.fromJson
import com.skedgo.tripkit.ui.utils.observe
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
import javax.inject.Inject

class PaymentSummaryFragment : BaseFragment<FragmentPaymentSummaryBinding>(), PaymentSummaryHandler {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val layoutRes: Int
        get() = R.layout.fragment_payment_summary

    override val observeAccessibility: Boolean
        get() = false

    @Inject
    lateinit var summaryDetailsListAdapter: PaymentSummaryDetailsListAdapter

    private val viewModel: PaymentSummaryViewModel by viewModels { viewModelFactory }

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onContinue() {
        viewModel.onContinueToPayment()
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().paymentComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {
        initViews()
        checkArguments()
        initObserver()
    }

    private fun initViews() {
        binding.rvDetails.adapter = summaryDetailsListAdapter
    }

    private fun checkArguments() {
        arguments?.let {
            if (it.containsKey(PaymentActivity.EXTRA_PAYMENT_DATA)) {
                viewModel.setData(
                        requireContext(),
                        gson.fromJson(it.getString(PaymentActivity.EXTRA_PAYMENT_DATA, ""))
                )
            }
        }
    }

    private fun initObserver() {
        viewModel.apply {
            observe(summaryDetails) {
                it?.let { summaryDetailsListAdapter.collection = it }
            }

            observe(loading) {
                it?.let { showLoading(it) }
            }

            observe(error) {
                it?.let {
                    requireContext().showConfirmationPopUpDialog(
                            title = "Error",
                            message = it.localizedMessage ?: it.message
                            ?: "Something went wrong. Please try again.",
                            "Close"
                    )
                }
            }

            observe(paymentIntent) {
                it?.let {
                    Log.e("MIKE", "payment intnet: ${gson.toJson(it)}")
                }
            }
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handler = this
    }

    companion object {

        fun newInstance(args: Bundle): PaymentSummaryFragment {
            return PaymentSummaryFragment().apply { arguments = args }
        }
    }
}