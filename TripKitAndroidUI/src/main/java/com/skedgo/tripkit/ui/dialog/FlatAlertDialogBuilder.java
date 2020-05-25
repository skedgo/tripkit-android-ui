package com.skedgo.tripkit.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.skedgo.tripkit.ui.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Was deprecated after setting min SDK 15.
 */
// TODO - Remove this class
@Deprecated
public class FlatAlertDialogBuilder {
  private int mPositiveTextResourceId = -1;
  private int mNegativeTextResourceId = -1;
  private int mContentViewResourceId = -1;
  private String mTitle;

  private Context mContext;
  private DialogInterface.OnClickListener mOnPositiveClickListener;
  private DialogInterface.OnClickListener mOnNegativeClickListener;
  private View mContentView;

  public FlatAlertDialogBuilder(Context context) {
    mContext = context;
  }

  public FlatAlertDialogBuilder setTitle(int titleTextResourceId) {
    mTitle = mContext.getString(titleTextResourceId);
    return this;
  }

  public FlatAlertDialogBuilder setTitle(String title) {
    mTitle = title;
    return this;
  }

  public FlatAlertDialogBuilder setPositiveButton(int positiveTextResourceId,
                                                  DialogInterface.OnClickListener onPositiveClickListener) {
    mPositiveTextResourceId = positiveTextResourceId;
    mOnPositiveClickListener = onPositiveClickListener;
    return this;
  }

  public FlatAlertDialogBuilder setNegativeButton(int negativeTextResourceId,
                                                  DialogInterface.OnClickListener onNegativeClickListener) {
    mNegativeTextResourceId = negativeTextResourceId;
    mOnNegativeClickListener = onNegativeClickListener;
    return this;
  }

  public View getContentView() {
    return mContentView;
  }

  public FlatAlertDialogBuilder setContentView(int contentViewResourceId) {
    mContentViewResourceId = contentViewResourceId;
    return this;
  }

  public Dialog create() {
    return createDialog();
  }

  private Dialog createDialog() {
    Dialog dialog = new Dialog(mContext, R.style.FlatDialog);

    View dialogView = dialog.getLayoutInflater().inflate(R.layout.v4_flat_dialog_part_container, null);
    if (dialogView != null) {
      TextView titleTextView = (TextView) dialogView.findViewById(R.id.v4_dialog_title);
      titleTextView.setText(mTitle);

      View buttonBar = dialogView.findViewById(R.id.toolbar);
      configureButtonBar(dialog, buttonBar);

      FrameLayout contentLayout = (FrameLayout) dialogView.findViewById(R.id.v4_dialog_content);
      mContentView = dialog.getLayoutInflater().inflate(mContentViewResourceId, contentLayout, false);
      if (mContentView != null) {
        contentLayout.addView(mContentView);
      }
    }

    dialog.setContentView(dialogView, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    return dialog;
  }

  private void configureButtonBar(final Dialog dialog, View buttonBar) {
    Button negativeButton = (Button) buttonBar.findViewById(R.id.v4_btn_negative);
    configureNegativeButton(dialog, negativeButton);

    Button positiveButton = (Button) buttonBar.findViewById(R.id.v4_btn_positive);
    configurePositiveButton(dialog, positiveButton);

    // Hides the button bar if there is no buttons visible anymore
    if (negativeButton.getVisibility() == View.GONE && positiveButton.getVisibility() == View.GONE) {
      buttonBar.setVisibility(View.GONE);
    }
  }

  private void configurePositiveButton(final Dialog dialog, Button positiveButton) {
    if (mPositiveTextResourceId != -1) {
      positiveButton.setText(mPositiveTextResourceId);
      positiveButton.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View view) {
          mOnPositiveClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
        }
      });
    } else {
      positiveButton.setVisibility(View.GONE);
    }
  }

  private void configureNegativeButton(final Dialog dialog, Button negativeButton) {
    if (mNegativeTextResourceId != -1) {
      negativeButton.setText(mNegativeTextResourceId);
      negativeButton.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View view) {
          mOnNegativeClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
        }
      });
    } else {
      negativeButton.setVisibility(View.GONE);
    }
  }

}