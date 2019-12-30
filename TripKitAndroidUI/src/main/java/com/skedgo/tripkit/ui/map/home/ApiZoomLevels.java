package com.skedgo.tripkit.ui.map.home;


/**
 * Zoom level that is compatible with the locations.json API
 */
public final class ApiZoomLevels {
  /**
   * Only non-parent stops (e.g, bus) are returned at this level.
   */
  public static final int LOCAL = 50;

  /**
   * Only parent stops (e.g, train) are returned at this level.
   */
  public static final int REGION = 1;
  public static final int UNKNOWN = 0;

  private ApiZoomLevels() {}

  /**
   * Converts zoom level defined by Google map into
   * zoom level compatible with the locations.json API.
   *
   * @param zoomLevel Zoom level defined by Google map.
   */
  public static int fromMapZoomLevel(ZoomLevel zoomLevel) {
    if (zoomLevel == ZoomLevel.INNER) {
      return LOCAL;
    }
    if (zoomLevel == ZoomLevel.OUTER) {
      return REGION;
    } else {
      return UNKNOWN;
    }
  }
}
