package com.skedgo.tripkit.analytics

import io.reactivex.Completable
import io.reactivex.Observable

class UserInfoRepositoryImpl : UserInfoRepository {

    private var plannedURL: String? = null
    private var userInfo: UserInfo? = null


    override fun clearUserInfo(plannedURL: String): Completable {
        return Completable.fromAction {
            this.plannedURL = null
            this.userInfo = null
        }
    }

    override fun setUserInfo(plannedURL: String, userInfo: UserInfo): Observable<Unit> {
        return Completable.fromAction {
            this.plannedURL = plannedURL
            this.userInfo = userInfo
        }.toObservable()
    }

    override fun getUserInfo(plannedURL: String): Observable<UserInfo> {
        return Observable.just(userInfo)
    }
}