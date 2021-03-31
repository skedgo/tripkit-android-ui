package com.skedgo.tripkit.ui.trippreview.standard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.button.MaterialButton
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.booking.FormField
import com.skedgo.tripkit.booking.LinkFormField
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerItemBinding
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.trippreview.nearby.SharedNearbyTripPreviewItemViewModel
import com.skedgo.tripkit.ui.trippreview.nearby.SharedNearbyTripPreviewItemViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import timber.log.Timber
import javax.inject.Inject


class StandardTripPreviewItemFragment : BaseTripKitFragment() {

    @Inject
    lateinit var sharedViewModelFactory: SharedNearbyTripPreviewItemViewModelFactory
    lateinit var sharedViewModel: SharedNearbyTripPreviewItemViewModel
    lateinit var vm: TripPreviewPagerItemViewModel
    lateinit var binding: TripPreviewPagerItemBinding
    @Inject
    lateinit var bookingService: BookingService

    var segment: TripSegment? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProviders.of(requireParentFragment(), sharedViewModelFactory).get("sharedNearbyViewModel", SharedNearbyTripPreviewItemViewModel::class.java)
        vm = ViewModelProviders.of(this).get(TripPreviewPagerItemViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TripPreviewPagerItemBinding.inflate(inflater)
        segment?.let {
            vm.setSegment(requireContext(), it)
        }
        binding.viewModel = vm
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        vm.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe{ onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        sharedViewModel.bookingForm.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    binding.actionButtonLayout.removeAllViews()
                    processForm(it)
                }
                .addTo(autoDisposable)

    }

    private fun processForm(form: BookingForm) {
        form.form.forEach {formGroup ->
            formGroup.fields.forEach { formField ->
                if (formField.id == "booking_status") {
                    vm.messageTitle.set(formField.title)
                    vm.message.set(formField.sidetitle)
                    vm.messageVisible.set(true)
                } else if (formField.id == "end_booking" && formField is LinkFormField) {
                    val newButton = MaterialButton(requireContext(), null, R.attr.borderlessButtonStyle)
                    newButton.text = formField.title
                    newButton.setOnClickListener {
                        runAction(formField)
                    }
                    binding.actionButtonLayout.addView(newButton)

                }

            }

        }
    }
    private fun runAction(formField: LinkFormField) {
        formField.value?.let {
            // This should actually be a post, though.
            bookingService.getFormAsync(it)
                    .observeOn(mainThread())
                    .subscribe({
                        sharedViewModel.bookingForm.accept(it)
                    }, {}).addTo(autoDisposable)
        }
    }

    companion object{
        fun newInstance(segment: TripSegment): StandardTripPreviewItemFragment{
            val fragment = StandardTripPreviewItemFragment()
            fragment.segment = segment
            return fragment
        }
    }
}