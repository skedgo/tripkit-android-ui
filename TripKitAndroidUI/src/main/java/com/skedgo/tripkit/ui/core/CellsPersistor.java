package com.skedgo.tripkit.ui.core;

import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.common.util.CollectionUtils;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.locations.LocationsResponse;
import com.skedgo.tripkit.data.locations.StopsFetcher;
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider;

import java.util.List;


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
      values.put(DbFields.CELL_CODE.getName(), cell.getKey());
      values.put(DbFields.HASH_CODE_2.getName(), cell.getHashCode());
      values.put(DbFields.DOWNLOAD_TIME.getName(), System.currentTimeMillis());

      valuesArray[i++] = values;
    }

    appContext.getContentResolver().bulkInsert(
        ScheduledStopsProvider.DOWNLOAD_HISTORY_URI,
        valuesArray
    );
  }
}