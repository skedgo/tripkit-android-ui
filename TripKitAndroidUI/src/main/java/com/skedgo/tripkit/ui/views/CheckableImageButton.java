package com.skedgo.tripkit.ui.views;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.view.AbsSavedState;

public class CheckableImageButton extends AppCompatImageButton implements Checkable {

    private static final int[] DRAWABLE_STATE_CHECKED = new int[]{android.R.attr.state_checked};

    private boolean checked;
    private boolean checkable = true;

    public CheckableImageButton(Context context) {
        this(context, null);
    }

    public CheckableImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.imageButtonStyle);
    }

    public CheckableImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ViewCompat.setAccessibilityDelegate(
            this,
            new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityEvent(View host, @NonNull AccessibilityEvent event) {
                    super.onInitializeAccessibilityEvent(host, event);
                    event.setChecked(isChecked());
                }

                @Override
                public void onInitializeAccessibilityNodeInfo(
                    View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setCheckable(isCheckable());
                    info.setChecked(isChecked());
                }
            });
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (checkable && this.checked != checked) {
            this.checked = checked;
            refreshDrawableState();
            sendAccessibilityEvent(AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
        }
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (checked) {
            return mergeDrawableStates(
                super.onCreateDrawableState(extraSpace + DRAWABLE_STATE_CHECKED.length),
                DRAWABLE_STATE_CHECKED);
        } else {
            return super.onCreateDrawableState(extraSpace);
        }
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.checked = checked;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.checked);
    }

    /**
     * Returns whether the image button is checkable.
     */
    public boolean isCheckable() {
        return checkable;
    }

    /**
     * Sets image button to be checkable or not.
     */
    public void setCheckable(boolean checkable) {
        if (this.checkable != checkable) {
            this.checkable = checkable;
            sendAccessibilityEvent(AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED);
        }
    }

    static class SavedState extends AbsSavedState {

        public static final Creator<SavedState> CREATOR =
            new ClassLoaderCreator<SavedState>() {
                @NonNull
                @Override
                public SavedState createFromParcel(@NonNull Parcel in, ClassLoader loader) {
                    return new SavedState(in, loader);
                }

                @NonNull
                @Override
                public SavedState createFromParcel(@NonNull Parcel in) {
                    return new SavedState(in, null);
                }

                @NonNull
                @Override
                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        boolean checked;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(@NonNull Parcel source, ClassLoader loader) {
            super(source, loader);
            readFromParcel(source);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(checked ? 1 : 0);
        }

        private void readFromParcel(@NonNull Parcel in) {
            checked = in.readInt() == 1;
        }
    }
}
