package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.skedgo.tripkit.common.model.Street
import com.skedgo.tripkit.ui.R


class GetInstructionIcon {
    fun getIcon(context: Context, street: Street): Drawable? {
        val result = when(street.instruction()) {
            Street.Instruction.HEAD_TOWARDS -> R.drawable.maneuver_start
            Street.Instruction.CONTINUE_STRAIGHT -> R.drawable.maneuver_go_straight
            Street.Instruction.TURN_SLIGHTLY_LEFT -> R.drawable.maneuver_light_left
            Street.Instruction.TURN_LEFT -> R.drawable.maneuver_quite_left
            Street.Instruction.TURN_SHARPLY_LEFT -> R.drawable.maneuver_heavy_left
            Street.Instruction.TURN_SLIGHTLY_RIGHT -> R.drawable.maneuver_light_right
            Street.Instruction.TURN_RIGHT -> R.drawable.maneuver_quite_right
            Street.Instruction.TURN_SHARPLY_RIGHT -> R.drawable.maneuver_heavy_right
            null -> R.drawable.maneuver_end
        }
        val drawable = AppCompatResources.getDrawable(context, result)
        drawable?.let {
            DrawableCompat.setTint(it, R.color.tripKitSuccess)
        }

        return drawable
    }
}