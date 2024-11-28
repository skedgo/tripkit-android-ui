package com.skedgo.tripkit.ui.data

import com.google.gson.annotations.JsonAdapter
import org.immutables.gson.Gson.TypeAdapters
import org.immutables.value.Value.Immutable
import org.immutables.value.Value.Parameter
import org.immutables.value.Value.Style
import org.immutables.value.Value.Style.ImplementationVisibility.PACKAGE

@Immutable
@TypeAdapters
@Style(visibility = PACKAGE, passAnnotations = [JsonAdapter::class])
@JsonAdapter(
    GsonAdaptersConfigDto::class
)
interface ConfigDto {
    @Parameter
    fun v(): String

    @Parameter
    fun tt(): Int?

    @Parameter
    fun ws(): Int?

    @Parameter
    fun cs(): Int?

    @Parameter
    fun conc(): Boolean?

    @Parameter
    fun wheelchair(): Boolean?

    @Parameter
    fun wp(): String?
}
