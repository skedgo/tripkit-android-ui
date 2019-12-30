package com.skedgo.tripkit.ui.core;

import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import com.skedgo.tripkit.common.util.StringUtils;
import com.skedgo.sqlite.Cursors;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.locations.LocationsResponse;
import com.skedgo.tripkit.data.locations.StopsFetcher;
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Function;

import java.util.List;

public class CellsLoader implements StopsFetcher.ICellsLoader {
  private final Context appContext;

  public CellsLoader(@NonNull Context appContext) {
    this.appContext = appContext;
  }

  @Override
  public Observable<List<LocationsResponse.Group>> loadSavedCellsAsync(
      @NonNull List<String> cellIds) {
    return querySavedCellsFromDatabaseAsync(appContext, cellIds)
        .flatMap(new Function<Cursor, Observable<LocationsResponse.Group>>() {
          @Override
          public Observable<LocationsResponse.Group> apply(Cursor cursor) {
            return toCellsAsync(cursor);
          }
        })
        .toList().toObservable();
  }

  @NonNull
  private Observable<Cursor> querySavedCellsFromDatabaseAsync(
      @NonNull final Context context,
      @NonNull final List<String> cellIds) {
    return Observable.create(new ObservableOnSubscribe<Cursor>() {
      @Override
      public void subscribe(ObservableEmitter<Cursor> emitter) throws Exception {
        final Cursor cursor = querySavedCellsFromDatabaseSync(context, cellIds);
        if (cursor != null) {
          emitter.onNext(cursor);
        }
        emitter.onComplete();
      }
    });
  }

  private Cursor querySavedCellsFromDatabaseSync(
      @NonNull Context context,
      @NonNull List<String> cellIds) {
    final String selection = String.format(
        "%s IN( %s )",
        DbFields.CELL_CODE,
        StringUtils.makeArgsString(cellIds.size())
    );

    final String[] selectionArgs = new String[cellIds.size()];
    for (int i = 0; i < cellIds.size(); i++) {
      selectionArgs[i] = cellIds.get(i);
    }

    return context.getContentResolver().query(
        ScheduledStopsProvider.DOWNLOAD_HISTORY_URI,
        null,
        selection,
        selectionArgs,
        null
    );
  }

  private Observable<LocationsResponse.Group> toCellsAsync(Cursor cursor) {
    return Observable.just(cursor)
        .flatMap(Cursors.flattenCursor())
        .map(new Function<Cursor, LocationsResponse.Group>() {
          @Override
          public LocationsResponse.Group apply(Cursor cursor) {
            final long hashCode = cursor.getLong(cursor.getColumnIndexOrThrow(DbFields.HASH_CODE_2.getName()));
            final String cellId = cursor.getString(cursor.getColumnIndexOrThrow(DbFields.CELL_CODE.getName()));
            return new LocationsResponse.Group(
                hashCode,
                cellId
            );
          }
        });
  }
}