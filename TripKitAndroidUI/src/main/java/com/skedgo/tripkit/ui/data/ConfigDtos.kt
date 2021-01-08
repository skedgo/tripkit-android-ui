package com.skedgo.tripkit.ui.data

object ConfigDtos {
  fun of(
      v: String = "11",
      tt: Int? = 3 /* minutes */,
      ws: Int? = 1 /* aka medium */,
      cs: Int? = 1 /* aka medium */,
      conc: Boolean? = false,
      wheelchair: Boolean? = false,
      wp: String? = "(1,1,1,1)"
  ): ConfigDto = ImmutableConfigDto.of(
      v, tt, ws, cs, conc, wheelchair, wp
  )
}