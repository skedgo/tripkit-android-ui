package com.skedgo.tripkit.ui.trippreview.standard

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.booking.LinkFormField
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerItemBinding
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.trippreview.nearby.SharedNearbyTripPreviewItemViewModel
import com.skedgo.tripkit.ui.trippreview.nearby.SharedNearbyTripPreviewItemViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import javax.inject.Inject
import kotlin.getValue


class StandardTripPreviewItemFragment : BaseFragment<TripPreviewPagerItemBinding>() {

    @Inject
    lateinit var sharedViewModelFactory: SharedNearbyTripPreviewItemViewModelFactory
    private val sharedViewModel: SharedNearbyTripPreviewItemViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { sharedViewModelFactory }
    )
    private val vm: TripPreviewPagerItemViewModel by viewModels()

    @Inject
    lateinit var bookingService: BookingService

    var segment: TripSegment? = null

    override val layoutRes: Int
        get() = R.layout.trip_preview_pager_item

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {
        segment?.let {
            vm.setSegment(requireContext(), it)
        }
        binding.lifecycleOwner = this
        binding.viewModel = vm
    }

    override fun onResume() {
        super.onResume()
        vm.closeClicked.observable.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        sharedViewModel.bookingForm.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling {
                binding.actionButtonLayout.removeAllViews()
                processForm(it)
            }
            .addTo(autoDisposable)

    }

    private fun processForm(form: BookingForm) {
        form.form.forEach { formGroup ->
            formGroup.fields.forEach { formField ->
                if (formField.id == "booking_status") {
                    vm.messageTitle.set(formField.title)
                    vm.message.set(formField.sidetitle)
                    vm.messageVisible.set(true)
                } else if (formField.id == "end_booking" && formField is LinkFormField) {
                    val newButton =
                        MaterialButton(requireContext(), null, R.attr.borderlessButtonStyle)
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
        formField.getValue()?.let {
            // This should actually be a post, though.
            bookingService.getFormAsync(it)
                .observeOn(mainThread())
                .subscribe({
                    sharedViewModel.bookingForm.accept(it)
                }, {}).addTo(autoDisposable)
        }
    }

    companion object {
        fun newInstance(segment: TripSegment): StandardTripPreviewItemFragment {
            val fragment = StandardTripPreviewItemFragment()
            fragment.segment = segment
            return fragment
        }
    }
}