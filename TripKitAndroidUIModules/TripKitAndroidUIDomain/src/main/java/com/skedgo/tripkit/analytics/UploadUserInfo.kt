package com.skedgo.tripkit.analytics

import javax.inject.Inject

open class UploadUserInfo @Inject internal constructor(
    val userInfoRepository: UserInfoRepository,
    val markTripAsPlannedWithUserInfo: MarkTripAsPlannedWithUserInfo
) {
//  open fun execute(plannedURL: String): Completable =
//      Completable.merge(
//          userInfoRepository.getUserInfo(plannedURL).toFlowable(BackpressureStrategy.BUFFER)
//              .map {
//                markTripAsPlannedWithUserInfo.execute(plannedURL, userInfo = it.toMutableMap())
//                    .andThen(userInfoRepository.clearUserInfo(plannedURL))
//              }
//      )
}
