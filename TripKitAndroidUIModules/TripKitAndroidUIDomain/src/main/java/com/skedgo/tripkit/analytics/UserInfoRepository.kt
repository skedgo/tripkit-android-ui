package com.skedgo.tripkit.analytics

import io.reactivex.Completable
import io.reactivex.Observable

interface UserInfoRepository {
    fun clearUserInfo(plannedURL: String): Completable
    fun setUserInfo(plannedURL: String, userInfo: UserInfo): Observable<Unit>

    /**
     * If there's no [UserInfo] present in the repository,
     * it returns an [Observable] which emits nothing.
     */
    fun getUserInfo(plannedURL: String): Observable<UserInfo>
}
