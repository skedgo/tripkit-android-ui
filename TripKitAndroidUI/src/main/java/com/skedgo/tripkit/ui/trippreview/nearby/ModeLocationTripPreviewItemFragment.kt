package com.skedgo.tripkit.ui.trippreview.nearby

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProviders
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.button.MaterialButton
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerModeLocationItemBinding
import com.skedgo.tripkit.ui.qrcode.INTENT_KEY_BARCODES
import com.skedgo.tripkit.ui.qrcode.INTENT_KEY_BUTTON_ID
import com.skedgo.tripkit.ui.qrcode.INTENT_KEY_INTERNAL_URL
import com.skedgo.tripkit.ui.qrcode.QrCodeScanActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

const val REQUEST_QR_SCAN = 1

class ModeLocationTripPreviewItemFragment(var segment: TripSegment) : BaseTripKitFragment() {
    private lateinit var binding: TripPreviewPagerModeLocationItemBinding

    @Inject
    lateinit var sharedViewModelFactory: SharedNearbyTripPreviewItemViewModelFactory

    lateinit var sharedViewModel: SharedNearbyTripPreviewItemViewModel

    @Inject
    lateinit var viewModel: ModeLocationTripPreviewViewModel

    @Inject
    lateinit var bookingService: BookingService

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProviders.of(parentFragment!!, sharedViewModelFactory).get("sharedNearbyViewModel", SharedNearbyTripPreviewItemViewModel::class.java)
        sharedViewModel.setSegment(context!!, segment)
        viewModel.set(segment)

        setBookingAction()
    }

    private fun setBookingAction() {
        sharedViewModel.withAction(isAppInstalled())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TripPreviewPagerModeLocationItemBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.sharedViewModel = sharedViewModel

        val layoutManager = FlexboxLayoutManager(context).apply {
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.CENTER
            flexDirection = FlexDirection.ROW
        }

        binding.infoGroupRecyclerView.layoutManager = layoutManager
        binding.infoGroupRecyclerView.isNestedScrollingEnabled = false

        if (!segment.booking?.confirmation?.actions().isNullOrEmpty()) {
            segment.booking.confirmation!!.actions().forEach { action ->
                if (sharedViewModel.bookingForm.value == null && action.type() == "UNLOCK") {
                    val newButton = MaterialButton(requireContext(), null, R.attr.materialButtonOutlinedStyle)
                    newButton.text = action.title()
                    newButton.id = action.hashCode()
                    newButton.setOnClickListener {
                        if (action.type() == "UNLOCK") {
                            action.internalURL()?.let {
                                launchScanner(newButton.id, it)
                            }
                        }
                    }

                    binding.buttonLayout.addView(newButton)
                }
            }
        }

        return binding.root
    }

    fun launchScanner(buttonId: Int, internalUrl: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.scan_qr_code)
        builder.setPositiveButton(R.string.scan) { dialogInterface: DialogInterface, i: Int ->
            var launchIntent = Intent(activity, QrCodeScanActivity::class.java)
            launchIntent.putExtra(INTENT_KEY_INTERNAL_URL, internalUrl)
            launchIntent.putExtra(INTENT_KEY_BUTTON_ID, buttonId)
            startActivityForResult(launchIntent, REQUEST_QR_SCAN)
        }
        builder.setNegativeButton(R.string.skip) { dialogInterface: DialogInterface, i: Int ->
            sendCode(buttonId, internalUrl, "test_qr_code")
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_QR_SCAN && resultCode == RESULT_OK) {
            data?.let {
                val barcodes = it.getStringArrayExtra(INTENT_KEY_BARCODES)?.firstOrNull()
                        ?: "unknown"
                val internalUrl = it.getStringExtra(INTENT_KEY_INTERNAL_URL)!!
                var buttonId = it.getIntExtra(INTENT_KEY_BUTTON_ID, -1)
                sendCode(buttonId, internalUrl, barcodes)
            }
        }
    }

    private fun sendCode(buttonId: Int, internalUrl: String, code: String) {
        if (buttonId > 0) {
            binding.buttonLayout.findViewById<Button>(buttonId).visibility = GONE
        }

        if (internalUrl.isNotEmpty()) {
            bookingService.postActionInputAsync(internalUrl, "qrcode", code)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        sharedViewModel.bookingForm.accept(it)
                    }
                    .addTo(autoDisposable)
        }
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.bookingForm
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { form ->
                    form.action?.let { action ->
                        if (action.isDone) {

                        }
                    }
                }
                .addTo(autoDisposable)
        sharedViewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        sharedViewModel.locationDetails.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewModel.set(it)
                }.addTo(autoDisposable)

        sharedViewModel.actionChosen.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (sharedViewModel.action == "openApp") {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getSharedVehicleIntentURI())))
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getSharedVehicleAppAndroidURL())))
                    }
                    tripPreviewPagerListener?.onServiceActionButtonClicked(sharedViewModel.action)
                }.addTo(autoDisposable)

        setBookingAction()

    }

    private fun getSharedVehicleIntentURI(): String? {
        return if (!segment.sharedVehicle?.operator()?.appInfo?.deepLink.isNullOrEmpty()) {
            segment.sharedVehicle.operator()?.appInfo?.deepLink
        } else if (!segment.sharedVehicle?.deepLink().isNullOrEmpty()) {
            segment.sharedVehicle.deepLink()
        } else {
            segment.sharedVehicle?.operator()?.website
        }
    }

    private fun getSharedVehicleDeepLink(): String? {
        return if (!segment.sharedVehicle?.operator()?.appInfo?.deepLink.isNullOrEmpty()) {
            segment.sharedVehicle.operator()?.appInfo?.deepLink
        } else {
            segment.sharedVehicle.deepLink()
        }
    }

    private fun getSharedVehicleAppAndroidURL(): String? {
        return if (!segment.sharedVehicle?.operator()?.appInfo?.appURLAndroid.isNullOrEmpty()) {
            segment.sharedVehicle.operator()?.appInfo?.appURLAndroid
        } else {
            segment.sharedVehicle.appURLAndroid()
        }
    }

    private fun isAppInstalled(): Boolean {
        val deepLink = getSharedVehicleIntentURI()
        if (deepLink.isNullOrEmpty()) {
            return false
        }

        if (getSharedVehicleDeepLink() == null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getSharedVehicleIntentURI()))
            val componentName = intent.resolveActivity(requireActivity().packageManager)
            if (componentName != null) {
                return true
            }
            return false
        }

        return try {
            val appUrl = getSharedVehicleAppAndroidURL()
            appUrl.let {
                val firstIndex = it!!.indexOf("=")
                val lastIndex = it.indexOf("&")
                requireActivity().packageManager.getPackageInfo(it.substring(firstIndex + 1, lastIndex), 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}