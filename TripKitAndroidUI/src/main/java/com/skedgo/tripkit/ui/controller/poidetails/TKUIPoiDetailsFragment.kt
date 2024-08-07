package com.skedgo.tripkit.ui.controller.poidetails

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentTkuiPoiDetailsBinding
import com.skedgo.tripkit.ui.poidetails.PoiDetailsFragment
import com.skedgo.tripkit.ui.utils.defocusAndHideKeyboard
import javax.inject.Inject

class TKUIPoiDetailsFragment : BaseFragment<FragmentTkuiPoiDetailsBinding>() {

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private var fragment: PoiDetailsFragment? = null
    private var location: Location? = null
    private var isRouting: Boolean = false
    private var isDeparture: Boolean = false

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_poi_details

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {

        val visible = view?.rootWindowInsets?.run {
            WindowInsetsCompat
                .toWindowInsetsCompat(this)
                .isVisible(WindowInsetsCompat.Type.ime())
        } ?: false

        if (visible) {
            defocusAndHideKeyboard(
                requireContext(), requireActivity().currentFocus ?: view?.rootView
            )
        }

        location?.let {
            fragment = PoiDetailsFragment.Builder(it)
                .showCloseButton(true)
                .isDeparture(isDeparture)
                .isRouting(isRouting)
                .build()

            fragment?.apply {
                setOnCloseButtonListener {
                    eventBus.publish(ViewControllerEvent.OnCloseAction())
                }
                this@TKUIPoiDetailsFragment.childFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, this)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        location?.let { loc ->
            fragment?.buttonClick?.subscribe {
                eventBus.publish(
                    ViewControllerEvent.OnLocationChosen(loc, LocationField.NONE)
                )
            }?.addTo(autoDisposable)
        }
    }

    fun updateData(location: Location) {
        fragment?.updateLocation(location)
    }

    companion object {
        const val TAG = "TKUIPoiDetailsFragment"

        @JvmStatic
        fun newInstance(location: Location, isRouting: Boolean, isDeparture: Boolean) =
            TKUIPoiDetailsFragment().apply {
                this.location = location
                this.isRouting = isRouting
                this.isDeparture = isDeparture
            }
    }
}