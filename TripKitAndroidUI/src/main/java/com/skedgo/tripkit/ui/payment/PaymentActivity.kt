package com.skedgo.tripkit.ui.payment

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.gson.Gson
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ActivityPaymentBinding
import com.skedgo.tripkit.ui.trippreview.drt.DrtItemViewModel
import com.skedgo.tripkit.ui.trippreview.drt.DrtTicketViewModel

class PaymentActivity : AppCompatActivity() {

    lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_payment)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = "Review Booking"
        }

        intent.extras?.let {
            if (it.containsKey(EXTRA_PAYMENT_DATA)) {
                val transaction = supportFragmentManager.beginTransaction()
                        .replace(
                                R.id.container,
                                PaymentSummaryFragment.newInstance(it)
                        )
                transaction.commit()
            } else {
                showErrorToastAndFinish()
            }
        } ?: kotlin.run {
            showErrorToastAndFinish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showErrorToastAndFinish() {
        Toast.makeText(this, "Sorry something went wrong", Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {

        /*
        const val EXTRA_DRT_ITEMS = "drt_items"
        const val EXTRA_DRT_TICKETS = "drt_tickets"
        */
        const val EXTRA_PAYMENT_DATA = "payment_data"

        /*
        fun getIntent(context: Context, drtItems: List<PaymentSummaryDetails>, drtTickets: List<PaymentSummaryDetails>): Intent {
            return Intent(context, PaymentActivity::class.java).apply {
                val gson = Gson()
                putExtra(EXTRA_DRT_ITEMS, gson.toJson(drtItems))
                putExtra(EXTRA_DRT_TICKETS, gson.toJson(drtTickets))
            }
        }
        */

        fun getIntent(context: Context, paymentData: PaymentData): Intent {
            return Intent(context, PaymentActivity::class.java).apply {
                putExtra(EXTRA_PAYMENT_DATA, Gson().toJson(paymentData))
            }
        }
    }
}