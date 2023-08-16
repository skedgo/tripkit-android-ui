package com.skedgo.tripkit.ui.controller.tripresultcontroller

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandlerFactory
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiTripResultsBinding
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.tripresults.TripResultListFragment
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class TKUITripResultsFragment : BaseFragment<FragmentTkuiTripResultsBinding>() {

    @Inject
    lateinit var tkuiActionButtonHandlerFactory: TKUIActionButtonHandlerFactory

    @Inject
    lateinit var routingConfig: GetRoutingConfig

    private var origin: Location? = null
    private var destination: Location? = null
    private var fromRouteCard: Boolean = true

    private var tripResultsCardFragment: TripResultListFragment? = null
    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null

    private val eventBus = ViewControllerEventBus

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_trip_results

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
    }

    override fun onCreated(savedInstance: Bundle?) {
        binding.lifecycleOwner = this
        initTripResults()
    }

    private fun initData() {
        if (actionButtonHandlerFactory == null) {
            actionButtonHandlerFactory = tkuiActionButtonHandlerFactory
        }

        runBlocking {
            val config = routingConfig.execute()
            val query = Query().apply {
                fromLocation = origin
                toLocation = destination
                unit = config.unit
                cyclingSpeed = config.cyclingSpeed.value
                walkingSpeed = config.walkingSpeed.value
                environmentWeight = config.weightingProfile.environmentPriority.value
                hassleWeight = config.weightingProfile.conveniencePriority.value
                budgetWeight = config.weightingProfile.budgetPriority.value
                timeWeight = config.weightingProfile.timePriority.value
                setTimeTag(TimeTag.createForLeaveNow())
            }

            tripResultsCardFragment = TripResultListFragment.Builder().withQuery(query)
                .withActionButtonHandlerFactory(actionButtonHandlerFactory!!).showCloseButton()
                .build()
        }
    }

    private fun initTripResults() {
        tripResultsCardFragment?.apply {
            this@TKUITripResultsFragment.childFragmentManager
                .beginTransaction()
                .replace(R.id.container, this).commit()

            setOnCloseButtonListener {
                eventBus.publish(ViewControllerEvent.OnCloseAction())
            }
            setOnTripSelectedListener { viewTrip: ViewTrip, list: List<TripGroup> ->
                eventBus.publish(ViewControllerEvent.OnViewTrip(viewTrip, list))
            }

            setOnLocationClickListener({
                originDestinationClickAction()
            }, {
                originDestinationClickAction()
            })
        }
    }

    private fun originDestinationClickAction() {
        if (fromRouteCard) {
            eventBus.publish(ViewControllerEvent.OnCloseAction())
        } else {
            eventBus.publish(ViewControllerEvent.OnShowRouteSelection(origin!!, destination!!))
        }
    }

    companion object {
        const val TAG = "TKUITripResultsFragment"

         fun newInstance(
             origin: Location, destination: Location, fromRouteCard: Boolean
         ) = TKUITripResultsFragment().apply {
             this.origin = origin
             this.destination = destination
             this.fromRouteCard = fromRouteCard
         }
    }
}