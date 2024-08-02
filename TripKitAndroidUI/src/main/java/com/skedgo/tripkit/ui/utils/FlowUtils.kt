package com.skedgo.tripkit.ui.utils

import com.skedgo.network.Resource
import com.skedgo.tripkit.BuildConfig
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import retrofit2.HttpException


fun <T> Flow<T>.merge(vararg flows: Flow<T>): Flow<T> = flowOf(*flows).flattenMerge()

suspend fun <T> FlowCollector<Resource<T>>.safeCall(
    errorHandlingCall: (suspend (Exception) -> Unit)? = null, //In case there's an extra handling when receiving error on call
    call: suspend () -> Unit
): FlowCollector<Resource<T>> {
    return this.apply {
        emit(Resource.loading(data = null))
        try {
            call.invoke()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }

            errorHandlingCall?.invoke(e) ?: kotlin.run {
                var code = -1
                if (e is HttpException) {
                    code = e.code()
                }
                emit(
                    Resource.error(
                        data = null,
                        message = e.message ?: "Error Occurred!",
                        code = code
                    )
                )
            }

        }
    }
}

fun <T> Single<T>.toFlow(): Flow<T> {
    return flow {
        try {
            emit(this@toFlow.blockingGet())
        } catch (e: Exception) {
            throw e
        }
    }
}