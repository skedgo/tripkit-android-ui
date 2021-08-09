package com.skedgo.tripkit.analytics

import com.skedgo.tripkit.ui.personaldata.MyPersonalDataRepository
import io.reactivex.Observable
import com.skedgo.tripkit.routing.Trip
import io.reactivex.Completable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class ReportPlannedTrip @Inject constructor(private val myPersonalDataRepository: MyPersonalDataRepository,
                                                 private val markTripAsPlannedWithUserInfo: MarkTripAsPlannedWithUserInfo) {
    open fun execute(
            selectedTrip: Trip,
            getChoiceSet: List<Choice>
    ): Observable<Unit> {
        return markTripAsPlannedWithUserInfo.execute(selectedTrip.plannedURL ?: "", userInfo = UserInfo(getChoiceSet).toMutableMap())
    }
}
