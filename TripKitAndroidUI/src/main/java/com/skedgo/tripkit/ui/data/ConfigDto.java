package com.skedgo.tripkit.ui.data;

import com.google.gson.annotations.JsonAdapter;

import org.immutables.gson.Gson.TypeAdapters;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.immutables.value.Value.Style;
import org.jetbrains.annotations.Nullable;

import static org.immutables.value.Value.Style.ImplementationVisibility.PACKAGE;

@Immutable
@TypeAdapters
@Style(
    visibility = PACKAGE,
    passAnnotations = JsonAdapter.class
)
@JsonAdapter(GsonAdaptersConfigDto.class)
public interface ConfigDto {
  @Parameter
  String v();

  @Parameter
  @Nullable
  Integer tt();

  @Parameter
  @Nullable
  Integer ws();

  @Parameter
  @Nullable
  Integer cs();

  @Parameter
  @Nullable
  Boolean conc();

  @Parameter
  @Nullable
  Boolean wheelchair();

  @Parameter
  @Nullable
  String wp();
}
