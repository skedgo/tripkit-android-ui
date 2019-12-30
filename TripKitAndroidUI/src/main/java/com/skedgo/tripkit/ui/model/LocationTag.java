package com.skedgo.tripkit.ui.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.ui.trip.options.SelectionType;

@Deprecated
public class LocationTag implements Parcelable {
  public static final Creator<LocationTag> CREATOR = new Creator<LocationTag>() {
    public LocationTag createFromParcel(Parcel source) {
      return new LocationTag(source);
    }

    public LocationTag[] newArray(int size) {
      return new LocationTag[size];
    }
  };

  private SelectionType type;
  private boolean isCurrentLocation;
  private Location location;

  public LocationTag() {}

  public LocationTag(Location location, SelectionType type) {
    this.setLocation(location);
    this.type = type;
  }

  protected LocationTag(Parcel parcel) {
    type = SelectionType.values()[parcel.readInt()];
    isCurrentLocation = parcel.readInt() == 1;
    if (parcel.dataAvail() > 0) {
      location = parcel.readParcelable(Location.class.getClassLoader());
    }
  }

  public LocationTag clone(Location newLocation) {
    final LocationTag clone = new LocationTag();
    clone.setLocation(newLocation);
    clone.type = type;
    clone.isCurrentLocation = isCurrentLocation;
    return clone;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt((type != null) ? type.ordinal() : SelectionType.ARRIVAL.ordinal());
    dest.writeInt(isCurrentLocation ? 1 : 0);
    if (getLocation() != null) {
      dest.writeParcelable(getLocation(), flags);
    }
  }

  /**
   * @return A query text used to search locations via geocoder.
   * @see <a href="https://redmine.buzzhives.com/issues/4186">Issue 4186</a>
   */
  @Nullable
  @Deprecated
  public String getQueryText() {
    return isCurrentLocation || getLocation() == null
        ? null
                /* Why? To prevent https://redmine.buzzhives.com/issues/4347. */
        : getLocation().getDisplayName();
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public boolean isCurrentLocation() {
    return isCurrentLocation;
  }

  public void setIsCurrentLocation(boolean isCurrentLocation) {
    this.isCurrentLocation = isCurrentLocation;
  }

  public SelectionType getType() {
    return type;
  }

  public void setType(SelectionType type) {
    this.type = type;
  }
}