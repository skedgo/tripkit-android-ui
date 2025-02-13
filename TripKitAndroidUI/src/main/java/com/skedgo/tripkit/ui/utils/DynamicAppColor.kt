package com.skedgo.tripkit.ui.utils

import com.skedgo.tripkit.account.data.AppColor
import com.skedgo.tripkit.account.data.AppColors

data class SystemColors(
    val barBackground: Int,
    val barForeground: Int,
    val tintColor: Int
)

/**
 * for dynamic color scheme
 * https://redmine.buzzhives.com/issues/23356
 */
object DynamicAppColor {
    private lateinit var systemColors: SystemColors
    private lateinit var appColors: AppColors

    fun initSystemColors(
        barBackground: Int,
        barForeground: Int,
        tintColor: Int
    ) {
        systemColors = SystemColors(barBackground, barForeground, tintColor)
    }

    fun initAppColors(
        barBackground: AppColor,
        barForeground: AppColor,
        tintColor: AppColor
    ) {
        this.appColors = AppColors(barBackground, barForeground, tintColor)
    }

    fun setAppColors(appColors: AppColors) {
        this.appColors = appColors
    }

    fun getAppColors(): AppColors? =
        if (this::appColors.isInitialized) {
            appColors
        } else {
            null
        }

    fun getSystemColors(): SystemColors? =
        if (this::systemColors.isInitialized) {
            systemColors
        } else {
            null
        }
}