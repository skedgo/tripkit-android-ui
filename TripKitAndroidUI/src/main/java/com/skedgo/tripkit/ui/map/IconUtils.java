package com.skedgo.tripkit.ui.map;

import android.content.res.Resources;
import android.util.DisplayMetrics;

public final class IconUtils {
  private IconUtils() {}

  public static String asUrl(Resources resources, String icon, String urlTemplate) {
    final String densityDpiName = getDensityDpiName(resources.getDisplayMetrics().densityDpi);
    return String.format(urlTemplate, densityDpiName, icon);
  }

  private static String getDensityDpiName(int densityDpi) {
    switch (densityDpi) {
      case DisplayMetrics.DENSITY_MEDIUM:
        return "mdpi";
      case DisplayMetrics.DENSITY_HIGH:
        return "hdpi";
      case DisplayMetrics.DENSITY_XHIGH:
        return "xhdpi";
      case DisplayMetrics.DENSITY_XXHIGH:
        return "xxhdpi";
      case DisplayMetrics.DENSITY_XXXHIGH:
      default:
        return "xxxhdpi";
    }
  }
}
