package com.skedgo.tripkit.ui.payment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemPaymentSummaryDetailsBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class PaymentSummaryDetailsListAdapter @Inject constructor() :
        RecyclerView.Adapter<PaymentSummaryDetailsListAdapter.Holder>(),
        AutoUpdatableAdapter {

    internal var collection: List<PaymentSummaryDetails> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.id == n.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder.from(
                    parent,
                    R.layout.item_payment_summary_details
            )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            item = collection[position]
            executePendingBindings()
        }
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ItemPaymentSummaryDetailsBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                        DataBindingUtil.inflate<ItemPaymentSummaryDetailsBinding>(
                                inflater, layout,
                                parent, false
                        )
                return Holder(binding)
            }
        }
    }
}
