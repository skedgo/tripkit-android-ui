package com.skedgo.tripkit.ui.map.home;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLngBounds;
import com.skedgo.tripkit.common.model.region.Region;
import com.skedgo.tripkit.common.util.StringUtils;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.location.GeoPoint;
import com.skedgo.tripkit.ui.data.CursorToStopConverter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public final class StopLoaderArgs {
    /**
     * FIXME: Why 75? This was taken from the iOS impl.
     * If we change into something else rather than 75,
     * it might produce cell ids incompatible with locations.json API.
     *
     * @see <a href="https://github.com/skedgo/tripgo-ios/blob/release-v4/Libraries/TripKit/TKCellHelper.m#L11">iOS impl</a>
     */
    public static final int CELLS_PER_DEGREE = 75;
    public static final String KEY_CELL_IDS = "cellIds";
    public static final String KEY_VISIBLE_BOUND = "visibleBounds";

    private StopLoaderArgs() {
    }

    @NonNull
    public static Pair<List<String>, LatLngBounds> newArgsForStopsLoader(
        @NonNull List<String> cellIds,
        @NonNull Region region,
        @NonNull LatLngBounds visibleBounds) {
        return new Pair<>(getCellIdsForLoading(cellIds, region), visibleBounds);
    }

    /**
     * @return A list of cell ids for regional level or local level
     * based on the zoom level of camera position
     */
    @NonNull
    public static ArrayList<String> getCellIdsByCameraZoom(
        @NonNull Region region,
        @NonNull GeoPoint geoPoint,
        float zoom,
        @NonNull LatLngBounds span) {
        if (ZoomLevel.fromLevel(zoom) == ZoomLevel.INNER) {
            return getCellIdsForLocalLevel(geoPoint, span);
        } else {
            return getCellIdsForRegionalLevel(region);
        }
    }

    /**
     * @return A list containing region name used as cell id for regional level
     */
    @NonNull
    public static ArrayList<String> getCellIdsForRegionalLevel(@NonNull Region region) {
        final ArrayList<String> ids = new ArrayList<>();
        ids.add(region.getName());
        return ids;
    }

    /**
     * @return A list of cell ids for local level
     */
    @NonNull
    public static ArrayList<String> getCellIdsForLocalLevel(
        @NonNull GeoPoint geoPoint,
        @NonNull LatLngBounds span) {
        final double latSpan = span.northeast.latitude - span.southwest.latitude;
        final double lonSpan = span.northeast.longitude - span.southwest.longitude;
        return getCellIdsForLocalLevel(
            geoPoint.getLatitude(),
            geoPoint.getLongitude(),
            latSpan,
            lonSpan
        );
    }

    @NonNull
    public static ArrayList<String> getCellIdsForLocalLevel(
        double lat,
        double lon,
        double latSpan,
        double lonSpan) {
        int minLat = (int) ((lat - (latSpan / 2)) * CELLS_PER_DEGREE - 1);
        int minLng = (int) ((lon - (lonSpan / 2)) * CELLS_PER_DEGREE - 1);
        int maxLat = (int) ((lat + (latSpan / 2)) * CELLS_PER_DEGREE);
        int maxLng = (int) ((lon + (lonSpan / 2)) * CELLS_PER_DEGREE);

        final String sharp = "#";
        final ArrayList<String> ids = new ArrayList<>();
        for (int latitude = minLat; latitude <= maxLat; latitude++) {
            for (int lng = minLng; lng <= maxLng; lng++) {
                ids.add(latitude + sharp + lng);
            }
        }

        return ids;
    }

    /**
     * Defines proper cell ids so that the loader can load up
     * stops that should be visible at corresponding level.
     * <p/>
     * If given cell ids are for the regional level, just return themselves.
     * Why? Because we expect that only parent stops are visible at this level.
     * However, if they are for the local level, should plus the cell ids for the regional level.
     * Why? Because we expect both parent stops and non-parent stops are visible at this level.
     */
    @NonNull
    public static ArrayList<String> getCellIdsForLoading(
        @NonNull List<String> cellIds,
        @NonNull Region region) {
        if (!cellIds.contains(region.getName())) {
            final ArrayList<String> result = new ArrayList<>(cellIds);
            result.addAll(getCellIdsForRegionalLevel(region));
            return result;
        } else {
            return new ArrayList<>(cellIds);
        }
    }

    public static String[] createStopLoaderSelectionArgs(
        @NonNull List<String> cellIds,
        @NonNull LatLngBounds visibleBounds) {
        double fromLng = Math.min(visibleBounds.southwest.longitude, visibleBounds.northeast.longitude);
        double toLng = Math.max(visibleBounds.southwest.longitude, visibleBounds.northeast.longitude);
        int cellIdSize = cellIds.size();
        final int selectionArgsLength = cellIdSize + 4;
        final String[] selectionArgs = new String[selectionArgsLength];
        for (int i = 0; i < cellIdSize; i++) {
            selectionArgs[i] = cellIds.get(i);
        }
        selectionArgs[selectionArgsLength - 4] = String.valueOf(visibleBounds.southwest.latitude);
        selectionArgs[selectionArgsLength - 3] = String.valueOf(visibleBounds.northeast.latitude);
        selectionArgs[selectionArgsLength - 2] = String.valueOf(fromLng);
        selectionArgs[selectionArgsLength - 1] = String.valueOf(toLng);
        return selectionArgs;
    }

    public static String createStopLoaderSelection(final int cellsIdSize) {
        final String visibleBoundSelection = " AND " + DbFields.LAT + " >= ? AND " +
            DbFields.LAT + " <= ? AND " +
            DbFields.LON + " >= ? AND " +
            DbFields.LON + " <= ?";
        return CursorToStopConverter.SELECTION_ALL.replace(
            CursorToStopConverter.REPLACE_WITH_VAR_ARGS,
            StringUtils.makeArgsString(cellsIdSize)
        ).concat(visibleBoundSelection);
    }
}
