package com.skedgo.tripkit.ui.trip.details.viewmodel

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.VehicleComponent
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import me.tatarka.bindingcollectionadapter2.ItemBinding
import javax.inject.Inject

open class OccupancyViewModel @Inject constructor(private val context: Context) {
    val drawableLeft = ObservableField<Drawable>()
    val occupancyText = ObservableField<String>()

    // For bus occupancy
    val hasOccupancySingleInformation = ObservableBoolean(false)
    val background = ObservableField<Drawable?>()

    // For train occupancy
    val hasOccupancyInformation = ObservableBoolean(false)
    val itemBinding = ItemBinding.of<TrainOccupancyItemViewModel>(BR.occupancy, R.layout.train_item)
    val items: ObservableList<TrainOccupancyItemViewModel> = ObservableArrayList()

    open fun setOccupancy(vehicle: RealTimeVehicle, showAverage: Boolean) {
        hasOccupancyInformation.set(vehicle.hasVehiclesOccupancy())
        val hasOccupancyInfo = vehicle.components.orEmpty().sumBy { it.size } > 0
        hasOccupancySingleInformation.set(hasOccupancyInfo && (vehicle.hasSingleVehicleOccupancy() || showAverage))
        if (vehicle.hasVehiclesOccupancy()) {
            showOccupancyForManyComponentsVehicle(vehicle.components!!)
        }

        vehicle.getAverageOccupancy()?.let {
            occupancyText.set(context.getString(GetTextForOccupancy.execute(it)))
            drawableLeft.set(
                ContextCompat.getDrawable(
                    context,
                    GetDrawableForOccupancy.execute(it)
                )
            )
        }
    }

    private fun showOccupancyForManyComponentsVehicle(vehicleComponents: List<List<VehicleComponent>>) {
        vehicleComponents
            .map {
                it.mapIndexed { index: Int, vehicleComponent: VehicleComponent ->
                    TrainOccupancyItemViewModel(
                        ContextCompat.getColor(
                            context,
                            GetColorForOccupancy.execute(vehicleComponent.getOccupancy())
                        ),
                        ContextCompat.getDrawable(
                            context,
                            when (index) {
                                it.lastIndex -> R.drawable.ic_train_head
                                else -> R.drawable.ic_train_carriage
                            }
                        )
                    )
                }
            }
            .flatten()
            .let {
                items.clear()
                items.addAll(it)
            }
    }

    fun hasInformation(): Boolean =
        hasOccupancyInformation.get() || hasOccupancySingleInformation.get()
}
