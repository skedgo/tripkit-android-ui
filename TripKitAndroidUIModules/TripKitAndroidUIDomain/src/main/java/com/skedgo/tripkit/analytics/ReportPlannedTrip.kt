package com.skedgo.tripkit.analytics

import com.skedgo.TripKit
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.ui.personaldata.MyPersonalDataRepository
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Reports a selected trip with choice-set analytics.
 *
 * Trip selection reporting is always sent. The personal data setting only
 * controls whether userToken is attached for de-duplication.
 */
open class ReportPlannedTrip @Inject constructor(
    private val myPersonalDataRepository: MyPersonalDataRepository,
    private val markTripAsPlannedWithUserInfo: MarkTripAsPlannedWithUserInfo
) {
    open fun execute(
        selectedTrip: Trip,
        getChoiceSet: List<Choice>
    ): Observable<Unit> {
        return myPersonalDataRepository.isUploadTripSelectionEnabled()
            .toObservable()
            .flatMap { shouldIncludeUserToken ->
                val userInfo = UserInfo(getChoiceSet).toMutableMap()
                if (shouldIncludeUserToken) {
                    val userToken = runCatching {
                        TripKit.getInstance().configs().userTokenProvider()?.call()
                    }.getOrNull()

                    if (!userToken.isNullOrBlank()) {
                        userInfo["userToken"] = userToken
                    }
                }

                markTripAsPlannedWithUserInfo.execute(
                    selectedTrip.plannedURL ?: "",
                    userInfo = userInfo
                )
            }
    }
}
