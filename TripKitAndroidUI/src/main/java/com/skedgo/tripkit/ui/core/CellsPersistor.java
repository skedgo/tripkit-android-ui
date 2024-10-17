package com.skedgo.tripkit.ui.core;

import android.content.ContentValues;
import android.content.Context;

import com.google.android.gms.common.util.CollectionUtils;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.locations.LocationsResponse;
import com.skedgo.tripkit.data.locations.StopsFetcher;
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider;

import java.util.List;

import androidx.annotation.NonNull;


public class CellsPersistor implements StopsFetcher.ICellsPersistor {
    private final Context appContext;

    public CellsPersistor(@NonNull Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public void saveCellsSync(@NonNull List<LocationsResponse.Group> cells) {
        final ContentValues[] valuesArray = new ContentValues[cells.size()];

        int i = 0;
        for (LocationsResponse.Group cell : cells) {
            if (CollectionUtils.isEmpty(cell.getStops())) {
                continue;
            }

            final ContentValues values = new ContentValues(3);
            values.put(DbFields.CELL_CODE.name, cell.getKey());
            values.put(DbFields.HASH_CODE_2.name, cell.getHashCode());
            values.put(DbFields.DOWNLOAD_TIME.name, System.currentTimeMillis());

            valuesArray[i++] = values;
        }

        appContext.getContentResolver().bulkInsert(
            ScheduledStopsProvider.DOWNLOAD_HISTORY_URI,
            valuesArray
        );
    }
}