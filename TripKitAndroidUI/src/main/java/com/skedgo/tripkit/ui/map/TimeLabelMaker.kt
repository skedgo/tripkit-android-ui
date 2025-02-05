package com.skedgo.tripkit.ui.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.skedgo.tripkit.common.util.DateTimeFormats;
import com.skedgo.tripkit.ui.R;

import androidx.annotation.NonNull;

public class TimeLabelMaker {
    private TextView timeTextView;
    private Canvas canvas;

    public TimeLabelMaker(@NonNull TextView timeTextView) {
        this.timeTextView = timeTextView;
    }

    public Bitmap create(long millis, String timeZone) {
        final String timeText = DateTimeFormats.printTime(
            timeTextView.getContext(),
            millis,
            timeZone
        );
        timeTextView.setText(timeText);
        measureTimeTextViewSize();

        Bitmap timeLabelBitmap = Bitmap.createBitmap(
            timeTextView.getMeasuredWidth(),
            timeTextView.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = getCanvas(timeLabelBitmap);
        timeTextView.draw(canvas);

        return timeLabelBitmap;
    }

    private void measureTimeTextViewSize() {
        timeTextView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            View.MeasureSpec.UNSPECIFIED
        );
        int heightSpec = View.MeasureSpec.makeMeasureSpec(
            timeTextView.getContext().getResources().getDimensionPixelSize(R.dimen.time_label_height),
            View.MeasureSpec.EXACTLY
        );
        timeTextView.measure(widthSpec, heightSpec);
        timeTextView.layout(0, 0, timeTextView.getMeasuredWidth(), timeTextView.getMeasuredHeight());
    }

    private Canvas getCanvas(Bitmap bitmap) {
        if (canvas == null) {
            canvas = new Canvas();
        }

        canvas.setBitmap(bitmap);
        return canvas;
    }
}