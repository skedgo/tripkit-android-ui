package com.skedgo.tripkit.ui.data.tripprogress;
import com.google.gson.annotations.JsonAdapter;

import java.util.List;

import static org.immutables.gson.Gson.TypeAdapters;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

@Immutable
@Style(
        overshadowImplementation = true,
        passAnnotations = JsonAdapter.class
)
@TypeAdapters
@JsonAdapter(GsonAdaptersUpdateProgressBody.class)
public abstract class UpdateProgressBody {

  public static Builder builder() {
    return new Builder();
  }

  public abstract List<LocationSampleDto> samples();

  public static class Builder extends ImmutableUpdateProgressBody.Builder {}
}
