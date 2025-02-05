package com.skedgo.tripkit.ui.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skedgo.tripkit.common.model.region.Region;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MapMarkerUtils {
    private static Paint circlePaint;

    static {
        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setFilterBitmap(true);
        circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        circlePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private MapMarkerUtils() {
    }

    public static Bitmap createStopMarkerIcon(
        int diameter,
        int strokeColor,
        int fillColor,
        boolean showOutline) {
        final Bitmap icon = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(icon);
        final int circleCenter = diameter / 2;
        if (showOutline) {
            circlePaint.setColor(Color.BLACK);
            canvas.drawCircle(circleCenter, circleCenter, diameter / 2, circlePaint);

            diameter = (int) (diameter * 0.9);
        }

        circlePaint.setColor(strokeColor);
        canvas.drawCircle(circleCenter, circleCenter, diameter / 2, circlePaint);

        final int innerDiameter = (int) (diameter * 0.7);
        circlePaint.setColor(fillColor);
        canvas.drawCircle(circleCenter, circleCenter, innerDiameter / 2, circlePaint);
        return icon;
    }

    /**
     * This replaces the old solution that use a transparent bitmap contained in the drawable/ folder.
     * The advantage is that, this creates a bitmap that looks more consistent over various DPIs.
     */
    public static BitmapDescriptor createTransparentSquaredIcon(@NonNull Resources res,
                                                                @DimenRes int sizeResId) {
        final int size = res.getDimensionPixelSize(sizeResId);
        return BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        ));
    }

    @Nullable
    public static MarkerOptions createCityMarker(Region.City city, BitmapDescriptor cityIcon) {
        return new MarkerOptions()
            .title(city.getName())
            .position(new LatLng(city.getLat(), city.getLon()))
            .icon(cityIcon)
            .draggable(false);
    }
}