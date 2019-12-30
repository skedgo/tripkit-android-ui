package com.skedgo.tripkit.ui.map;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.utils.ViewUtils;

/**
 * View to create custom callout in {@link InfoWindowAdapter},
 * simply including a title, a snippet, a left image and a right image.
 */
public final class SimpleCalloutView extends LinearLayout {
  private TextView titleView;
  private TextView snippetView;
  private ImageView rightImageView;
  private ImageView leftImageView;

  /**
   * Should not invoke this constructor directly.
   * Use {@link #create(LayoutInflater)} instead.
   */
  public SimpleCalloutView(Context context) {
    super(context);
  }

  public SimpleCalloutView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SimpleCalloutView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public SimpleCalloutView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public static SimpleCalloutView create(@NonNull LayoutInflater inflater) {
    return (SimpleCalloutView) inflater.inflate(R.layout.view_simple_callout, null);
  }

  public void setTitle(@Nullable String title) {
    ViewUtils.setText(titleView, title);
  }

  public void setSnippet(@Nullable String snippet) {
    ViewUtils.setText(snippetView, snippet);
  }

  public void setLeftImage(@DrawableRes int res) {
    ViewUtils.setImage(leftImageView, res);
  }

  public void setRightImage(@DrawableRes int res) {
    ViewUtils.setImage(rightImageView, res);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    titleView = findViewById(R.id.titleView);
    snippetView = findViewById(R.id.snippetView);
    rightImageView = findViewById(R.id.rightImageView);
    leftImageView = findViewById(R.id.leftImageView);
  }
}