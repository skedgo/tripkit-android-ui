package com.skedgo.tripkit.ui.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import com.skedgo.tripkit.common.model.RealtimeAlert;
import com.skedgo.tripkit.common.model.ScheduledStop;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://redmine.buzzhives.com/projects/buzzhives/wiki/RealTime_API#DepartureServlet-new-servlet-departuresjson">departures.json API</a>
 */
public class DeparturesResponse {
  /**
   * (Optional)
   */
  @SerializedName("error")
  public String error;

  /**
   * (Optional)
   */
  @SerializedName("usererror")
  public boolean hasError;

  @SerializedName("embarkationStops")
  public List<ServicesResponse> embarkationStopList;

  /**
   * (Optional)
   */
  @SerializedName("stops")
  public List<ScheduledStop> stopList;

  /**
   * (Optional)
   */
  @SerializedName("parentInfo")
  private ScheduledStop parentInfo;

  /**
   * (Optional)
   */
  @SerializedName("alerts")
  private List<RealtimeAlert> alerts;

  private List<TimetableEntry> mServiceList;

  @Nullable
  public ScheduledStop getParentInfo() {
    return parentInfo;
  }

  /**
   * Assigns stop code into corresponding service
   * <p/>
   * NOTE: Must call this method after parsing the response
   */
  public void processEmbarkationStopList() {
    if (embarkationStopList != null) {
      for (DeparturesResponse.ServicesResponse servicesResponse : embarkationStopList) {
        if (servicesResponse.serviceList != null) {
          for (TimetableEntry service : servicesResponse.serviceList) {
            if (service != null) {
              service.setStopCode(servicesResponse.stopCode);
            }
          }
        }
      }
    }
  }

  @Nullable
  public List<TimetableEntry> getServiceList() {
    if (mServiceList == null) {
      mServiceList = extractServiceList();
    }

    return mServiceList;
  }

  @Nullable
  public List<RealtimeAlert> getAlerts() {
    return alerts;
  }

  public void setAlerts(List<RealtimeAlert> alerts) {
    this.alerts = alerts;
  }

  /**
   * Extracts services from the embarkation stops
   */
  @Nullable
  private List<TimetableEntry> extractServiceList() {
    if (embarkationStopList == null) {
      return null;
    }

    List<TimetableEntry> serviceList = new ArrayList<TimetableEntry>();
    for (ServicesResponse servicesResponse : embarkationStopList) {
      if (servicesResponse.serviceList != null) {
        serviceList.addAll(servicesResponse.serviceList);
      }
    }

    return serviceList;
  }

  public static class ServicesResponse {
    @SerializedName("stopCode")
    public String stopCode;

    @SerializedName("services")
    public List<TimetableEntry> serviceList;
  }
}
