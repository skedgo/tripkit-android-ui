package com.skedgo.tripkit.ui.trippreview.nearby

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerNearbyItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject


class NearbyTripPreviewItemFragment(var segment: TripSegment) : BaseTripKitFragment() {
    @Inject
    lateinit var sharedViewModelFactory: SharedNearbyTripPreviewItemViewModelFactory
    lateinit var sharedViewModel: SharedNearbyTripPreviewItemViewModel
    lateinit var viewModel: NearbyTripPreviewItemViewModel

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get("nearbyViewModel", NearbyTripPreviewItemViewModel::class.java)
        sharedViewModel = ViewModelProviders.of(parentFragment!!, sharedViewModelFactory).get("sharedNearbyViewModel", SharedNearbyTripPreviewItemViewModel::class.java)
        sharedViewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        sharedViewModel.setSegment(context!!, segment)

        setBookingAction()
    }

    private fun setBookingAction() {
        sharedViewModel.withAction(isAppInstalled())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewPagerNearbyItemBinding.inflate(inflater)
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        binding.transportItemsView.layoutManager = layoutManager
        binding.lifecycleOwner = this
        binding.sharedViewModel = sharedViewModel
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearTransportModes()

        sharedViewModel.locationList
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewModel.setLocations(it)
                }.addTo(autoDisposable)
        sharedViewModel.locationList
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapIterable { item -> item }
                .filter { it.modeInfo != null }
                .map { location -> location.modeInfo!! }
                .distinct { it.id }
                .subscribe { mode -> viewModel.addMode(mode) }
                .addTo(autoDisposable)

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