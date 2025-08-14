package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.text.format.DateFormat

/**
 * Singleton class to manage system time format detection.
 * This eliminates the need to pass Context around to classes that don't have access to it.
 */
object SystemTimeFormatManager {
    
    private var applicationContext: Context? = null
    
    /**
     * Initialize the manager with the Application context.
     * Should be called in Application.onCreate()
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    /**
     * Check if the system is using 24-hour format.
     * @return true if 24-hour format is enabled, false for 12-hour format
     */
    fun is24HourFormat(): Boolean {
        val context = applicationContext
        return if (context != null) {
            DateFormat.is24HourFormat(context)
        } else {
            // Default to 24-hour format if not initialized
            true
        }
    }
    
    /**
     * Get the appropriate time format pattern based on system settings.
     * @return "H:mm" for 24-hour format, "h:mm a" for 12-hour format
     */
    fun getTimeFormatPattern(): String {
        return if (is24HourFormat()) "H:mm" else "h:mm a"
    }
    
    /**
     * Get the appropriate time format pattern with AM/PM indicator for 12-hour format.
     * @return "H:mm" for 24-hour format, "h:mm aa" for 12-hour format
     */
    fun getTimeFormatPatternWithAmPm(): String {
        return if (is24HourFormat()) "H:mm" else "h:mm aa"
    }
    
    /**
     * Get a date-time format pattern with the appropriate time format.
     * @param datePattern The date pattern (e.g., "MMM dd, yyyy")
     * @return Combined date-time pattern with system-aware time format
     */
    fun getDateTimeFormatPattern(datePattern: String): String {
        return "$datePattern ${getTimeFormatPattern()}"
    }
    
    /**
     * Get a date-time format pattern with AM/PM indicator.
     * @param datePattern The date pattern (e.g., "MMM dd, yyyy")
     * @return Combined date-time pattern with system-aware time format and AM/PM
     */
    fun getDateTimeFormatPatternWithAmPm(datePattern: String): String {
        return "$datePattern ${getTimeFormatPatternWithAmPm()}"
    }
}
