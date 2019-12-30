package com.skedgo.tripkit.account

import io.reactivex.Observable

interface UserTokenRepository {
  /**
   * If there is no user token, this returns an empty [Observable].
   */
  fun getLastKnownUserToken(): Observable<UserToken>

  fun getUserTokenByUserIdentifier(userIdentifier: String): Observable<UserToken>
  fun getUserTokenBySignUpCredentials(signUpCredentials: SignUpCredentials): Observable<UserToken>
  fun getUserTokenBySignInCredentials(signInCredentials: SignInCredentials): Observable<UserToken>
  fun clearUserToken(): Observable<Boolean>
  fun clearUserTokenByLoggingOut(): Observable<Boolean>
  fun onUserTokenChanged(): Observable<Any>
}
