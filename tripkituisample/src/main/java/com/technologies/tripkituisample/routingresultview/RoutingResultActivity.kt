package com.technologies.tripkituisample.routingresultview

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.skedgo.TripKit
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.BaseActivity
import com.skedgo.tripkit.ui.tripresult.TripResultPagerFragment
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment
import com.skedgo.tripkit.ui.tripresults.TripResultListFragment
import com.technologies.tripkituisample.*
import com.technologies.tripkituisample.databinding.ActivityRoutingResultBinding
import javax.inject.Inject

class RoutingResultActivity : BaseActivity<ActivityRoutingResultBinding>() {

    @Inject
    lateinit var eventBus: AppEventBus

    private lateinit var viewModel: RoutingResultViewModel

    override val layoutRes: Int
        get() = R.layout.activity_routing_result

    override fun onCreated(instance: Bundle?) {
        viewModel = ViewModelProvider(this)[RoutingResultViewModel::class.java]
        initObservers()
        viewModel.getRouteQuery()
    }

    private fun initObservers() {

        val globalConfigs = TripKit.getInstance().configs() as TripKitUISampleConfigs
        val actionButtonHandlerFactory = globalConfigs.actionButtonHandlerFactory()

        viewModel.query.observe(this) { query ->

            val tripResultListFragmentBuilder = TripResultListFragment.Builder()
                .withQuery(query)
                .showCloseButton()

            if(actionButtonHandlerFactory != null) {
                tripResultListFragmentBuilder.withActionButtonHandlerFactory(
                    actionButtonHandlerFactory
                )
            }

            val tripResultListFragment = tripResultListFragmentBuilder.build()

            tripResultListFragment.onCloseButtonListener = View.OnClickListener {
                finish()
            }

            tripResultListFragment.setOnTripSelectedListener { viewTrip, tripGroups ->
                val pagerFragmentBuilder = TripResultPagerFragment.Builder()
                    .showCloseButton()

                pagerFragmentBuilder.withViewTrip(viewTrip)

                if(actionButtonHandlerFactory != null) {
                    pagerFragmentBuilder
                        .withActionButtonHandlerFactory(actionButtonHandlerFactory)
                }

                val sortedList = ArrayList<TripGroup>()
                tripGroups.forEach {
                    if (it.uuid().equals(viewTrip.tripGroupUUID)) {
                        sortedList.add(0, it)
                    } else {
                        sortedList.add(it)
                    }
                }
                pagerFragmentBuilder.withInitialTripGroupList(sortedList)

                val pagerFragment: TripResultPagerFragment = pagerFragmentBuilder.build()
                pagerFragment.setOnCloseButtonListener {
                    supportFragmentManager.popBackStack()
                }
                pagerFragment.setOnTripUpdatedListener { trip ->

                    trip?.group?.let {
                        val list = ArrayList<TripGroup>()
                        list.add(it)
                    }
                }

                pagerFragment.tripSegmentClickListener =
                    object : TripSegmentListFragment.OnTripSegmentClickListener {
                        override fun tripSegmentClicked(tripSegment: TripSegment) {}
                    }

                setFragment(pagerFragment)
            }

            setFragment(tripResultListFragment)
        }
    }

    private fun setFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .replace(R.id.container, fragment)
            .commit()
    }
}