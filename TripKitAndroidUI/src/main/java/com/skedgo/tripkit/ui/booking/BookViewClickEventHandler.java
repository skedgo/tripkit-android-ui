package com.skedgo.tripkit.ui.booking;

import android.content.DialogInterface;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.skedgo.tripkit.routing.TripSegment;

import java.util.List;


public final class BookViewClickEventHandler {
  private final Fragment fragment;

  private BookViewClickEventHandler(@NonNull Fragment fragment) {
    this.fragment = fragment;
  }

  public static BookViewClickEventHandler create(@NonNull Fragment fragment) {
    return new BookViewClickEventHandler(fragment);
  }

  @NonNull
  static String[] getActionTitles(List<String> externalActions, BookingResolver bookingResolver) {
    final String[] titles = new String[externalActions.size()];
    for (int i = 0; i < externalActions.size(); i++) {
      final String title = bookingResolver.getTitleForExternalAction(externalActions.get(i));
      titles[i] = TextUtils.isEmpty(title) ? externalActions.get(i) : title;
    }

    return titles;
  }
//
//  @Subscribe
//  public void onEvent(BookViewClickEvent event) {
//    final Booking booking = event.segment.getBooking();
//
//    final BookingResolver bookingResolver = TripKitUI.getInstance().getBookingResolver();
//    final List<String> externalActions = booking.getExternalActions();
//    if (isNotEmpty(externalActions)) {
//      if (externalActions.size() == 1) {
//        performAction(bookingResolver, externalActions.get(0), event.segment);
//      } else {
//        showActionsDialog(bookingResolver, event.segment);
//      }
//    }
//
//  }

  private void performAction(
      @NonNull BookingResolver bookingResolver,
      final String action,
      @NonNull final TripSegment segment) {
    // @TODO Do we need this in the SDK?
//
//    final ExternalActionParams params = ExternalActionParams.builder()
//        .action(action)
//        .segment(segment)
//        .build();
//    bookingResolver.performExternalActionAsync(params)
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe(new Action1<BookingAction>() {
//          @Override public void call(final BookingAction bookingAction) {
//            if (bookingAction.bookingProvider() == BookingResolver.SMS) {
//              if (bookingAction.data().resolveActivity(fragment.getActivity().getPackageManager()) != null) {
//                fragment.startActivity(bookingAction.data());
//
//                // This is when booking with SMS.
//                // Record the event for Analytics due to
//                // requirement of https://redmine.buzzhives.com/issues/6030.
//
//                /* FIXME: Use EventTracker to track this event.
//                BaseApp.component().tracker().send(
//                    new HitBuilders.EventBuilder()
//                        .setCategory("goalFlow")
//                        .setAction("BookSMS")
//                        .setLabel("start")
//                        .build()); */
//              }
//            } else {
//              fragment.startActivity(bookingAction.data());
//            }
//          }
//        }, ErrorHandlers.errorTracker());
  }

  private void showActionsDialog(
      @NonNull final BookingResolver bookingResolver,
      @NonNull final TripSegment segment) {
    final List<String> externalActions = segment.getBooking().getExternalActions();
    if (externalActions != null) {
      final String[] titles = getActionTitles(externalActions, bookingResolver);
      new AlertDialog.Builder(fragment.getActivity())
          .setItems(titles, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
              performAction(bookingResolver, externalActions.get(which), segment);
            }
          })
          .create()
          .show();
    }
  }
}