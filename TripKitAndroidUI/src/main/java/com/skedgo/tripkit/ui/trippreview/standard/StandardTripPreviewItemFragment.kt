package com.skedgo.tripkit.ui.trippreview.standard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.booking.LinkFormField
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
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
    private val vm: TripPreviewPagerItemViewModel by viewModels()

    @Inject
    lateinit var bookingService: BookingService

    var segment: TripSegment? = null
    private var messageTitleState by mutableStateOf("")
    private var messageState by mutableStateOf("")
    private var messageVisibleState by mutableStateOf(false)
    private val actionButtons = mutableStateListOf<LinkFormField>()

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireParentFragment(), sharedViewModelFactory)
            .get("sharedNearbyViewModel", SharedNearbyTripPreviewItemViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        segment?.let {
            vm.setSegment(requireContext(), it)
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripKitUITheme {
                    StandardTripPreviewScreen(
                        viewModel = vm,
                        messageTitle = messageTitleState,
                        message = messageState,
                        messageVisible = messageVisibleState,
                        actionButtons = actionButtons,
                        onActionClicked = ::runAction
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.closeClicked.observable.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        sharedViewModel.bookingForm.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling {
                processForm(it)
            }
            .addTo(autoDisposable)

    }

    private fun processForm(form: BookingForm) {
        actionButtons.clear()
        messageVisibleState = false
        form.form.forEach { formGroup ->
            formGroup.fields.forEach { formField ->
                if (formField.id == "booking_status") {
                    vm.messageTitle.set(formField.title)
                    vm.message.set(formField.sidetitle)
                    vm.messageVisible.set(true)
                    messageTitleState = formField.title.orEmpty()
                    messageState = formField.sidetitle.orEmpty()
                    messageVisibleState = true
                } else if (formField.id == "end_booking" && formField is LinkFormField) {
                    actionButtons.add(formField)
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