package com.skedgo.tripkit.ui.core

import com.haroldadmin.cnradapter.NetworkResponse
import timber.log.Timber


fun <T : Any, U : Any> NetworkResponse<T, U>.logError() {
    when (this) {
        is NetworkResponse.ServerError -> Timber.e("Error: Network response ${this.code}")
        is NetworkResponse.Success -> Timber.d("Successful response: ${this.code}")
        is NetworkResponse.NetworkError -> Timber.e("Network error", this.error)
        is NetworkResponse.UnknownError -> Timber.e("Unknown error", this.error)
    }
}

fun <T : Any, U : Any> NetworkResponse<T, U>.getDisplayError(): String {
    return when (this) {
        is NetworkResponse.ServerError -> "Network response ${this.code}"
        is NetworkResponse.Success -> "Successful response: ${this.code}"
        is NetworkResponse.NetworkError -> "Error: ${this.error.message}"
        is NetworkResponse.UnknownError -> "Error: ${this.error.message}"
    }

}