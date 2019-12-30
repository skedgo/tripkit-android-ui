package com.skedgo.tripkit.ui.data.tripprogress;

import com.google.gson.annotations.JsonAdapter;

import org.immutables.gson.Gson.TypeAdapters;
import org.immutables.value.Value.Immutable;

import static org.immutables.value.Value.Style;
import static org.immutables.value.Value.Style.ImplementationVisibility.PACKAGE;

@Immutable
@Style(
    overshadowImplementation = true,
    passAnnotations = JsonAdapter.class
)
@TypeAdapters
@JsonAdapter(GsonAdaptersLocationSampleDto.class)
public interface LocationSampleDto {
  long timestamp();
  double latitude();
  double longitude();
  int bearing();
  int speed();

  class Builder extends ImmutableLocationSampleDto.Builder {}
}
