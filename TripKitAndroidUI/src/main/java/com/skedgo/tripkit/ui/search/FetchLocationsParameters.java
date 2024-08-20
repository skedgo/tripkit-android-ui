package com.skedgo.tripkit.ui.search;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.BuilderVisibility;

import static org.immutables.value.Value.Style.ImplementationVisibility.PRIVATE;

@Value.Immutable
@Value.Style(
    visibility = PRIVATE,
    builderVisibility = BuilderVisibility.PACKAGE
)
public abstract class FetchLocationsParameters {
    public static Builder builder() {
        return new FetchLocationsParametersBuilder();
    }

    public abstract double northeastLat();

    public abstract double northeastLon();

    public abstract double southwestLat();

    public abstract double southwestLon();

    public abstract double nearbyLat();

    public abstract double nearbyLon();

    public abstract String term();

    public interface Builder {
        Builder northeastLat(double northeastLat);

        Builder northeastLon(double northeastLon);

        Builder southwestLat(double southwestLat);

        Builder southwestLon(double southwestLon);

        Builder nearbyLat(double nearbyLat);

        Builder nearbyLon(double nearbyLon);

        Builder term(String term);

        FetchLocationsParameters build();
    }
}
