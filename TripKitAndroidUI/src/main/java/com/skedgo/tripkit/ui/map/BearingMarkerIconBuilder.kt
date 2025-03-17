package com.skedgo.tripkit.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.skedgo.tripkit.ui.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class BearingMarkerIconBuilder {
    private boolean mHasBearing;
    private int mBearing;
    private boolean mHasBearingVehicleIcon;
    private int mBaseIconResourceId;
    private int mPointerIconResourceId;
    private boolean mHasTime;
    private long mMillis;
    private Context mContext;
    private TimeLabelMaker mTimeLabelMaker;
    private Paint mRotationPaint;
    private Drawable vehicleIconRes;
    private float vehicleIconScale = 1f;
    private String mTimezone;

    public BearingMarkerIconBuilder(@NonNull Context context,
                                    TimeLabelMaker timeLabelMaker) {
        mContext = context;
        mTimeLabelMaker = timeLabelMaker;

        mRotationPaint = new Paint();
        mRotationPaint.setAntiAlias(true);
        mRotationPaint.setFilterBitmap(true);
    }

    public BearingMarkerIconBuilder hasBearing(boolean hasBearing) {
        mHasBearing = hasBearing;
        return this;
    }

    public BearingMarkerIconBuilder bearing(int bearing) {
        mBearing = bearing;
        return this;
    }

    /**
     * @param vehicleIconRes Must be an instance of {@link BitmapDrawable}.
     */
    public BearingMarkerIconBuilder vehicleIcon(Drawable vehicleIconRes) {
        this.vehicleIconRes = vehicleIconRes;
        return this;
    }

    public BearingMarkerIconBuilder vehicleIconScale(float vehicleIconScale) {
        this.vehicleIconScale = vehicleIconScale;
        return this;
    }

    public BearingMarkerIconBuilder baseIcon(@DrawableRes int baseIconResourceId) {
        mBaseIconResourceId = baseIconResourceId;
        return this;
    }

    public BearingMarkerIconBuilder pointerIcon(@DrawableRes int pointerIconResourceId) {
        mPointerIconResourceId = pointerIconResourceId;
        return this;
    }

    public BearingMarkerIconBuilder hasTime(boolean hasTime) {
        mHasTime = hasTime;
        return this;
    }

    public BearingMarkerIconBuilder time(long millis, String timeZone) {
        mMillis = millis;
        mTimezone = timeZone;
        return this;
    }

    public BearingMarkerIconBuilder hasBearingVehicleIcon(boolean hasBearingVehicleIcon) {
        mHasBearingVehicleIcon = hasBearingVehicleIcon;
        return this;
    }

    public Pair<Bitmap, Float> build() {
        Bitmap vehiclePointerBitmap = createVehiclePointerBitmap();
        Bitmap vehiclePointerPinBitmap = createVehiclePointerPinBitmap(vehiclePointerBitmap);
        vehiclePointerBitmap.recycle();

        if (mHasTime && mTimeLabelMaker != null) {
            Pair<Bitmap, Float> markerIcon = plusTimeLabel(vehiclePointerPinBitmap);
            vehiclePointerPinBitmap.recycle();
            return markerIcon;
        } else {
            return new Pair<>(vehiclePointerPinBitmap, 0.5f);
        }
    }

    private Pair<Bitmap, Float> plusTimeLabel(Bitmap vehiclePointerPinBitmap) {
        Bitmap timeLabelBitmap = mTimeLabelMaker.create(mMillis, mTimezone);
        Bitmap finalBitmap = Bitmap.createBitmap(
            vehiclePointerPinBitmap.getWidth() + timeLabelBitmap.getWidth(),
            vehiclePointerPinBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(finalBitmap);
        int rotateAngle = convertToCanvasAxes(mBearing);
        boolean bearingToWesternSide = isBearingToWesternSide(rotateAngle);

        int offset = mContext.getResources().getDimensionPixelSize(R.dimen.v4_content_padding);
        int timeLabelLeft = bearingToWesternSide
            ? vehiclePointerPinBitmap.getWidth() - offset
            : offset;
        float timeLabelTop = (vehiclePointerPinBitmap.getWidth() - timeLabelBitmap.getHeight()) / 2f;

        final Drawable timeLabelBackgroundDrawable = ContextCompat.getDrawable(mContext, R.drawable.v4_shape_map_time_label);
        if (timeLabelBackgroundDrawable != null) {
            canvas.save();
            {
                float timeLabelBackgroundLeft = bearingToWesternSide
                    ? vehiclePointerPinBitmap.getWidth() / 2f
                    : offset;
                canvas.translate(timeLabelBackgroundLeft, timeLabelTop);

                timeLabelBackgroundDrawable.setBounds(
                    0, 0,
                    timeLabelBitmap.getWidth() + vehiclePointerPinBitmap.getWidth() / 2 - offset,
                    timeLabelBitmap.getHeight()
                );
                timeLabelBackgroundDrawable.draw(canvas);
            }
            canvas.restore();
        }

        canvas.drawBitmap(timeLabelBitmap, timeLabelLeft, timeLabelTop, null);
        timeLabelBitmap.recycle();

        int vehiclePointerPinBitmapLeft = !bearingToWesternSide
            ? finalBitmap.getWidth() - vehiclePointerPinBitmap.getWidth()
            : 0;
        canvas.drawBitmap(vehiclePointerPinBitmap, vehiclePointerPinBitmapLeft, 0, null);

        float anchor = vehiclePointerPinBitmap.getWidth() / (2.f * finalBitmap.getWidth());
        float anchorU = bearingToWesternSide ? anchor : 1.0f - anchor;
        return new Pair<>(finalBitmap, anchorU);
    }

    private Bitmap createVehiclePointerPinBitmap(Bitmap vehiclePointerBitmap) {
        // Padding between the VehiclePointer and the PinBase
        int padding = -mContext.getResources().getDimensionPixelSize(R.dimen.v4_base_pointer_padding) * 2;
        Bitmap baseBitmap = BitmapFactory.decodeResource(mContext.getResources(), mBaseIconResourceId);
        Bitmap vehiclePointerPinBitmap = Bitmap.createBitmap(
            vehiclePointerBitmap.getWidth(),
            vehiclePointerBitmap.getHeight() + padding + baseBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(vehiclePointerPinBitmap);

        // Locate the base
        int baseLeft = (vehiclePointerBitmap.getWidth() - baseBitmap.getWidth()) / 2;
        int baseTop = vehiclePointerBitmap.getHeight() + padding;
        canvas.drawBitmap(baseBitmap, baseLeft, baseTop, null);
        canvas.drawBitmap(vehiclePointerBitmap, 0, 0, null);

        return vehiclePointerPinBitmap;
    }

    private Bitmap createVehiclePointerBitmap() {
        Bitmap pointerBitmap = BitmapFactory.decodeResource(mContext.getResources(), mPointerIconResourceId);
        Bitmap vehiclePointerBitmap = Bitmap.createBitmap(
            pointerBitmap.getWidth(),
            pointerBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(vehiclePointerBitmap);

        int rotateAngle = convertToCanvasAxes(mBearing);
        if (mHasBearing) {
            canvas.save();

            // Rotate the Pointer bitmap to reflect bearing
            canvas.rotate(
                rotateAngle,
                vehiclePointerBitmap.getWidth() / 2,
                vehiclePointerBitmap.getHeight() / 2
            );
            canvas.drawBitmap(pointerBitmap, 0, 0, mRotationPaint);

            canvas.restore();
        } else {
            canvas.drawBitmap(pointerBitmap, 0, 0, null);
        }

        pointerBitmap.recycle();

        // Place the Vehicle bitmap onto the Pointer bitmap
        canvas.save();

        Bitmap vehicleBitmap = convertDrawableToBitmap(vehicleIconRes);
        if (mHasBearing && mHasBearingVehicleIcon && isBearingToWesternSide(rotateAngle)) {
            // Flip drawing horizontally to reflect bearing: East -> West
            canvas.scale(-1.0f, 1.0f);
            canvas.translate(-vehiclePointerBitmap.getWidth(), 0f);
        }

        int vehicleBitmapLeft = (vehiclePointerBitmap.getWidth() - vehicleBitmap.getWidth()) / 2;
        int vehicleBitmapTop = (vehiclePointerBitmap.getHeight() - vehicleBitmap.getHeight()) / 2;
        canvas.drawBitmap(vehicleBitmap, vehicleBitmapLeft, vehicleBitmapTop, null);

        vehicleBitmap.recycle();

        canvas.restore();
        return vehiclePointerBitmap;
    }

    private boolean isBearingToWesternSide(int rotateAngle) {
        return rotateAngle >= 90 && rotateAngle <= 270;
    }

    private int convertToCanvasAxes(int travelDirection) {
        return 360 - correctTravelDirection(travelDirection);
    }

    /**
     * Recomputes travel direction against Pointer's default direction (90 in degrees)
     */
    private int correctTravelDirection(int travelDirection) {
        if (travelDirection >= 0 && travelDirection <= 90) {
            return 90 - travelDirection;
        } else if (travelDirection > 90 && travelDirection <= 180) {
            return Math.abs(travelDirection - 360 - 90);
        } else if (travelDirection > 180 && travelDirection <= 360) {
            return Math.abs(travelDirection - 360) + 90;
        } else {
            return travelDirection;
        }
    }

    private Bitmap convertDrawableToBitmap(Drawable drawable) {
        final Bitmap bitmap = Bitmap.createBitmap(
            drawable.getIntrinsicWidth(),
            drawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888
        );

        final Canvas canvas = new Canvas(bitmap);
        canvas.scale(
            vehicleIconScale,
            vehicleIconScale,
            bitmap.getWidth() / 2,
            bitmap.getHeight() / 2
        );

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}