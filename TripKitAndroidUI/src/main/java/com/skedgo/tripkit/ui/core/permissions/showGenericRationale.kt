package com.skedgo.tripkit.ui.core.permissions

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import io.reactivex.Single

fun Activity.showGenericRationale(
    title: String? = null,
    message: String
): () -> Single<ActionResult> = {
    Single.create {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                it.onSuccess(ActionResult.Cancel)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> it.onSuccess(ActionResult.Proceed) }
            .setOnCancelListener { _ -> it.onSuccess(ActionResult.Cancel) }
            .show()
        it.setCancellable() {
            dialog.dismiss()
        }
    }
}
