package com.skedgo.tripkit.ui.tripresult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.getBookingSegment
import com.skedgo.tripkit.ui.*
import com.skedgo.tripkit.ui.ARG_TRIP_GROUP_ID
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripSegmentListFragmentBinding
import com.skedgo.tripkit.ui.model.TripKitButton
import com.skedgo.tripkit.ui.model.TripKitButtonConfigurator
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.squareup.otto.Bus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import kotlinx.android.synthetic.main.trip_segment_list_fragment.view.*
import timber.log.Timber
import javax.inject.Inject


class TripSegmentListFragment : BaseTripKitFragment(), View.OnClickListener {
    private val RQ_VIEW_TIMETABLE = 0
    private val RQ_VIEW_ALERTS = 1

    override fun onClick(p0: View?) {
//        if (mClickListener != null && p0 != null) {
//            mClickListener?.tripKitButtonClicked(p0.id, viewModel.tripGroup)
//        }
    }

    interface OnTripKitButtonClickListener {
        fun tripKitButtonClicked(id: Int, tripGroup: TripGroup)
    }

    private var mClickListener: OnTripKitButtonClickListener? = null
    fun setOnTripKitButtonClickListener(callback: OnTripKitButtonClickListener) {
        this.mClickListener = callback
    }

    fun setOnTripKitButtonClickListener(callback:(Int, TripGroup) -> Unit) {
        this.mClickListener = object: OnTripKitButtonClickListener {
            override fun tripKitButtonClicked(id: Int, tripGroup: TripGroup) {
                callback(id, tripGroup)
            }
        }
    }


    interface OnTripSegmentClickListener {
        fun tripSegmentClicked(tripSegment: TripSegment)
    }
    private var onTripSegmentClickListener: OnTripSegmentClickListener? = null
    fun setOnTripSegmentClickListener(callback: OnTripSegmentClickListener) {
        this.onTripSegmentClickListener = callback
    }

    fun setOnTripSegmentClickListener(callback:(TripSegment) -> Unit) {
        this.onTripSegmentClickListener = object: OnTripSegmentClickListener {
            override fun tripSegmentClicked(tripSegment: TripSegment) {
                callback(tripSegment)
            }

        }
    }


    @Inject
    lateinit var errorLogger: ErrorLogger

    @Inject
    lateinit var viewModel: TripSegmentsViewModel

    lateinit var binding: TripSegmentListFragmentBinding
    private var tripGroupId: String? = null
    private var tripId: Long? = null
    var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripDetailsComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            tripGroupId = requireArguments().getString(ARG_TRIP_GROUP_ID)
            tripId = requireArguments().getLong(ARG_TRIP_ID)
        } else if (savedInstanceState != null) {
            tripGroupId = savedInstanceState.getString(ARG_TRIP_GROUP_ID)
            tripId = savedInstanceState.getLong(ARG_TRIP_ID)

        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tripGroupId?.let {
            viewModel.loadTripGroup(it, tripId ?: -1, savedInstanceState)
        }

        viewModel.tripGroupObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {tripGroup ->
                    tripGroup.displayTrip?.getBookingSegment()?.booking?.externalActions?.forEach {

                    }
                }.addTo(autoDisposable)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {

        binding = TripSegmentListFragmentBinding.inflate(inflater)
        binding.viewModel = viewModel
        viewModel.setActionButtonHandlerFactory(actionButtonHandlerFactory)
        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        viewModel.showCloseButton.set(showCloseButton)
        binding.closeButton.setOnClickListener(onCloseButtonListener)
        binding.itemsView.isNestedScrollingEnabled = true
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
//        viewModel.tripGroupObservable
//                .observeOn(mainThread())
//                .take(1)
//                .subscribe { tripGroup ->
//                    buttonConfigurator?.let { configurator ->
//                        binding.buttonLayout.forEach {
//                            configurator.configureButton(context!!, it, tripGroup)
//                        }
//                    }
//
//                }.addTo(autoDisposable)
    }

    override fun onResume() {
        super.onResume()
        viewModel.segmentClicked
                .subscribe {
                    onTripSegmentClickListener?.tripSegmentClicked(it)
                }.addTo(autoDisposable)
        viewModel.alertsClicked
                .subscribe {
                    var list = mutableListOf<TripSegmentAlertsItemViewModel>()
                    it.forEach { alert ->
                        val vm = TripSegmentAlertsItemViewModel()
                        vm.titleText.set(alert.title())
                        vm.descriptionText.set(alert.text())
                        list.add(vm)
                    }
                    val dialog = TripSegmentAlertsSheet.newInstance(list)
                    dialog.show(requireFragmentManager(), "alerts_sheet")
                }.addTo(autoDisposable)
    }
    override fun onStop() {
        super.onStop()
        viewModel!!.onStop()
    }

    //  @Subscribe
    //  public void onEvent(SegmentInfoWindowClickEvent event) {
    //    final int position = viewModel.findSegmentPosition(event.getSegment());
    //    if (position != -1) {
    //      if (itemsView != null) {
    //        itemsView.smoothScrollToPosition(position);
    //      }
    //    }
    //  }
    //
    //  @Subscribe
    //  public void onEvent(ViewTimetableButtonClickEvent event) {
    //    // Show the Timetable screen
    //    startActivityForResult(ViewTimetableActivity.newIntent(
    //        getActivity(),
    //        event.scheduledSegment
    //    ), RQ_VIEW_TIMETABLE);
    //  }
    //
    //  @Subscribe
    //  public void onEvent(ShowAlertsEvent event) {
    //    startActivityForResult(ViewAlertsActivity.newIntent(
    //        getActivity(), new ArrayList<>(event.getAlerts())
    //    ), RQ_VIEW_ALERTS);
    //  }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            //      if (requestCode == RQ_VIEW_ALERTS) {
            //        final RealtimeAlert selectedAlert = data.getParcelableExtra(ViewAlertsActivity.KEY_SELECTED_ALERT);
            //        final View view = getView();
            //        if (view != null) {
            //          // A trick to avoid event loss.
            //          // This ensures the listening view controllers can receive
            //          // the event after onResume() which is the point of bus registration.
            //          view.post(() -> bus.post(new AlertClickEvent(selectedAlert)));
            //        }
            //      } else if (requestCode == RQ_AUTH) {
            //        if (data != null) {
            //          BookingForm form = data.getParcelableExtra(KEY_FORM);
            //          if (form != null) {
            //            viewModel.updateBookingForm(form);
            //          } else {
            //            viewModel.updateBookingForm();
            //          }
            //        }
        } else if (requestCode == RQ_VIEW_TIMETABLE) {
            //        final TimetableEntry selectedService = data.getParcelableExtra(SegmentTimetableFragment.KEY_SELECTED_SERVICE);
            //        final long segmentId = data.getLongExtra(SegmentTimetableFragment.KEY_SEGMENT_ID, -1);
            //        viewModel.onAlternativeTimeForTripSegmentSelected(segmentId, selectedService)
            //            .observeOn(AndroidSchedulers.mainThread())
            //            .compose(bindToLifecycle().forCompletable())
            //            .subscribe(() -> {
            //            }, (error -> {
            //              Toast.makeText(getContext(), R.string.an_unexpected_network_error_has_occurred_dot_please_retry_dot, Toast.LENGTH_LONG).show();
            //              errorLogger.trackError(error);
            //            }));
            //      }
        }

        //    if (requestCode == RQ_UBER_SSO) {
        //      LoginManager loginManager = getLoginManager();
        //      loginManager.onActivityResult(getActivity(), requestCode, resultCode, data);
        //    }
    }

    //  @NonNull
    //  private LoginManager getLoginManager() {
    //    final Context context = BaseApp.component().appContext();
    //    AccessTokenManager accessTokenManager = new AccessTokenManager(context);
    //    return new LoginManager(accessTokenManager, new LoginCallback() {
    //      @Override public void onLoginCancel() {}
    //
    //      @Override public void onLoginError(@NonNull AuthenticationError error) {
    //        Toast.makeText(context, error.name(), Toast.LENGTH_SHORT).show();
    //      }
    //
    //      @Override public void onLoginSuccess(@NonNull AccessToken accessToken) {
    //        viewModel.handleLoginSuccess(accessToken)
    //            .doOnError(errorLogger::trackError)
    //            .filter(bookingForm -> bookingForm != null)
    //            .subscribe(bookingForm -> {
    //              if (bookingForm.externalAction() != null) {
    //                Intent intent = ExternalProviderAuthActivity.newIntent(getActivity(), bookingForm);
    //                startActivityForResult(intent, RQ_AUTH);
    //              } else {
    //                viewModel.updateBookingForm(bookingForm);
    //              }
    //            }, error -> {
    //              Toast.makeText(context, R.string.there_was_an_error_and_it_was_reported_to_us_dot_please_try_again, Toast.LENGTH_SHORT).show();
    //            });
    //      }
    //
    //      @Override public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {}
    //    }, UberSsoHandler.Companion.createUberSessionConfig(context), RQ_UBER_SSO);
    //  }

    //  private void requestAlternativeTimes() {
    //    getFragmentManager().beginTransaction().add(
    //        AlternativeTripsFragment
    //            .newInstance(viewModel.getTripGroup().uuid()), null)
    //        .commitAllowingStateLoss();
    //  }
    //
    //  private void showQRCode(BookingConfirmation confirmation) {
    //    startActivity(QrCodeViewerActivity.newIntent(
    //        getActivity(),
    //        confirmation,
    //        viewModel.getTimeZoneId()
    //    ));
    //  }

    private fun showMessage(info: String) {
        val builder = AlertDialog.Builder(requireActivity())
                .setMessage(info)
                .setPositiveButton(R.string.ok) { dialog, which -> dialog.dismiss() }
        builder.show()
    }

    class Builder {
        private var tripGroupId : String? = null
        private var tripId: Long? = null
        private var buttons: List<TripKitButton>? = null
        private var buttonConfigurator: TripKitButtonConfigurator? = null
        private var showCloseButton = false
        private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
        fun withTripGroupId(tripGroupId: String): Builder {
            this.tripGroupId = tripGroupId
            return this
        }

        fun withTripId(tripId: Long?): Builder {
            tripId?.let {
                this.tripId = it
            }
            return this
        }
        fun withActionButtonHandlerFactory(factory: ActionButtonHandlerFactory?) : Builder {
            this.actionButtonHandlerFactory = factory
            return this
        }


        fun showCloseButton(showCloseButton: Boolean): Builder {
            this.showCloseButton = showCloseButton
            return this
        }

        fun build(): TripSegmentListFragment {
            assert(tripGroupId != null)

            val args = Bundle()
            args.putString(ARG_TRIP_GROUP_ID, tripGroupId)
            args.putLong(ARG_TRIP_ID, tripId ?: -1)
            args.putBoolean(ARG_SHOW_CLOSE_BUTTON, showCloseButton)

            // Initialize fragment
            val fragment = TripSegmentListFragment()
            fragment.arguments = args
            fragment.actionButtonHandlerFactory = actionButtonHandlerFactory
            return fragment

        }
    }
}