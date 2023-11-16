package com.skedgo.tripkit.ui.servicedetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemServiceAlertBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class AlertsAdapter @Inject constructor() :
    RecyclerView.Adapter<AlertsAdapter.Holder>(),
    AutoUpdatableAdapter {

    var collection: List<RealtimeAlert> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.title() == n.title() && o.text() == n.text() }
    }

    var clickListener: (RealtimeAlert) -> Unit = { _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder.from(
            parent,
            R.layout.item_service_alert
        )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            item = collection[position]
            executePendingBindings()
            holder.itemView.setOnClickListener {
                clickListener.invoke(collection[position])
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ItemServiceAlertBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    DataBindingUtil.inflate<ItemServiceAlertBinding>(
                        inflater, layout,
                        parent, false
                    )
                return Holder(binding)
            }
        }
    }
}

interface AlertClickListener {
    fun onAlertClick(alert: RealtimeAlert)
}

@BindingAdapter("alerts", "alertClickListener")
fun setAlerts(
    view: RecyclerView,
    alerts: List<RealtimeAlert>?,
    listener: AlertClickListener?
) {
    if (!alerts.isNullOrEmpty()) {
        view.adapter = AlertsAdapter().apply {
            collection = alerts
            clickListener = { listener?.onAlertClick(it) }
        }
    }
}