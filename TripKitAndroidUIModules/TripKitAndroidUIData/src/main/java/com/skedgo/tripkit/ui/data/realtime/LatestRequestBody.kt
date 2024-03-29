package com.skedgo.tripkit.ui.data.realtime

import org.immutables.gson.Gson
import com.google.gson.annotations.JsonAdapter
import org.immutables.value.Value

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersLatestRequestBody::class)
interface LatestRequestBody {
  fun region(): String

  fun services(): List<LatestService>
}
