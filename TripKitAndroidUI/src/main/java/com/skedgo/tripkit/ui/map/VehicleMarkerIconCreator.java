package com.skedgo.tripkit.ui.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.skedgo.tripkit.ui.R;

import javax.inject.Inject;

public class VehicleMarkerIconCreator {
    private final Resources resources;
    private final Paint paint;
    private final Paint borderPaint;
    private final Paint textPaint;
    private final int strokeWidth;

    @Inject
    public VehicleMarkerIconCreator(Resources resources) {
        this.resources = resources;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        strokeWidth = resources.getDimensionPixelSize(R.dimen.vehicleMarkerBorderWidth);
        borderPaint.setStrokeWidth(strokeWidth);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setDither(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.vehicleMarkerTextSize));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public Bitmap call(int bearing, int color, String text) {
        final int height = resources.getDimensionPixelSize(R.dimen.vehicleMarkerHeight);
        final int width = resources.getDimensionPixelSize(R.dimen.vehicleMarkerWidth);

        // Make it squared to increase touch target.
        final Bitmap icon = Bitmap.createBitmap(height, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(icon);
        final float center = height / 2f;

        paint.setColor(color);

        // Draw pyramid part.
        canvas.save();
        {
            canvas.translate((height - width) / 2.0f, 0);

            // Compute this to prevent the stroke from being cropped.
            // This is due to the fact that the paint draws the line in center mode.
            final float halfStrokeWidth = strokeWidth / 2f;
            final Path path = new Path();
            path.moveTo(width / 2.0f, halfStrokeWidth);
            path.lineTo(width, height * 0.25f);
            path.lineTo(width, height - halfStrokeWidth);
            path.lineTo(0, height - halfStrokeWidth);
            path.lineTo(0, height * 0.25f);
            path.lineTo(width / 2.0f, halfStrokeWidth);
            path.close();

            canvas.drawPath(path, paint);
            canvas.drawPath(path, borderPaint);
        }
        canvas.restore();

        // Draw text.
        canvas.save();
        {
            // Rotate to draw text the correct direction.
            canvas.rotate(-90.0f, center, center);

            // Make sure we always show the text the right way up.
            final boolean shouldFlip = bearing > 180.0f && bearing < 360.0f;
            if (shouldFlip) {
                canvas.rotate(180.0f, center, center);
            }

            // Following technique was taken from https://chris.banes.me/2014/03/27/measuring-text/.
            final Rect textBounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
            int textWidth = textBounds.width();
            int textHeight = textBounds.height();

            final float arrowPadding = height * 0.25f;
            canvas.drawText(
                text,
                center - textWidth * 0.5f + arrowPadding,
                center + (textHeight / 2f),
                textPaint
            );
        }
        canvas.restore();
        return icon;
    }
}