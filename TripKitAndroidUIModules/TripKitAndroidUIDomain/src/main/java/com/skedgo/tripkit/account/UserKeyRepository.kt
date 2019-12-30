package com.skedgo.tripkit.account

import io.reactivex.Single

interface UserKeyRepository {
  fun getUserKey(): Single<String>
}
