package com.skedgo.tripkit.ui.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.google.android.gms.common.util.CollectionUtils;
import com.google.gson.Gson;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.ScheduledStop;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.locations.LocationsResponse;
import com.skedgo.tripkit.data.locations.StopsFetcher;
import com.skedgo.tripkit.ui.map.ScheduledStopRepository;
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class StopsPersistor implements StopsFetcher.IStopsPersistor {
    private static final int INSERT_BATCH_SIZE = 300;

    private final Context appContext;
    private final Gson gson;
    private ScheduledStopRepository scheduledStopRepository;

    public StopsPersistor(@NonNull Context appContext,
                          @NonNull Gson gson,
                          @NonNull ScheduledStopRepository scheduledStopRepository) {
        this.appContext = appContext;
        this.gson = gson;
        this.scheduledStopRepository = scheduledStopRepository;
    }

    @Override
    public void saveStopsSync(@NonNull List<LocationsResponse.Group> cells) {
        final List<ContentValues> stopValuesList = new ArrayList<>();
        final List<ContentValues> locationValuesList = new ArrayList<>();

        for (LocationsResponse.Group cell : cells) {
            final String cellId = cell.getKey();
            final List<ScheduledStop> stops = cell.getStops();
            if (cellId == null || CollectionUtils.isEmpty(stops)) {
                continue;
            }

            createContentValues(
                cellId,
                getCodeToIdMapping(cellId),
                stops,
                stopValuesList,
                locationValuesList
            );
        }

        final int coreCount = Runtime.getRuntime().availableProcessors();
        final long sleepTime;
        if (coreCount >= 4) {
            // Quad-core phones can handle it!
            sleepTime = 0;
        } else if (coreCount >= 2) {
            // Getting weaker.
            sleepTime = 200;
        } else {
            // OMG, single core devices still exist?!
            sleepTime = 600;
        }

        if (stopValuesList.size() == locationValuesList.size()) {
            insertInBatches(stopValuesList, locationValuesList, sleepTime);
            stopValuesList.clear();
            locationValuesList.clear();
        }
    }

    private void createContentValues(String cellCode,
                                     Map<String, Integer> codeToIdMap, List<ScheduledStop> stops,
                                     List<ContentValues> scheduledStopValues,
                                     List<ContentValues> locationValues) {
        if (cellCode != null && stops != null) {
            Iterator<ScheduledStop> iter = stops.iterator();

            final Random r = new Random(System.currentTimeMillis());
            while (iter.hasNext()) {
                ScheduledStop stop = iter.next();

                Integer existingId = codeToIdMap == null ? null : codeToIdMap
                    .get(stop.getCode());
                final int parentStopId = existingId == null ? r
                    .nextInt(Integer.MAX_VALUE) : existingId;

                final ContentValues parentStopValues = new ContentValues(8);
                parentStopValues.put(DbFields.ID.getName(), parentStopId);
                parentStopValues.put(DbFields.STOP_TYPE.getName(), stop
                    .getType() == null ? null : stop.getType().toString());
                parentStopValues.put(DbFields.CELL_CODE.getName(), cellCode);
                parentStopValues.put(DbFields.CODE.getName(), stop.getCode());
                parentStopValues.put(DbFields.SHORT_NAME.getName(),
                    stop.getShortName());
                parentStopValues.put(DbFields.SERVICES.getName(),
                    stop.getServices());
                parentStopValues.put(DbFields.MODE_INFO.getName(),
                    gson.toJson(stop.getModeInfo()));
                parentStopValues.put(DbFields.IS_PARENT.getName(),
                    stop.hasChildren() ? 1 : 0);

                scheduledStopValues.add(parentStopValues);

                final ContentValues parentLocationValues = new ContentValues(9);
                parentLocationValues.put(DbFields.SCHEDULED_STOP_CODE.getName(),
                    stop.getCode());
                parentLocationValues
                    .put(DbFields.NAME.getName(), stop.getName());
                parentLocationValues.put(DbFields.ADDRESS.getName(),
                    stop.getAddress());
                parentLocationValues.put(DbFields.LAT.getName(), stop.getLat());
                parentLocationValues.put(DbFields.LON.getName(), stop.getLon());
                parentLocationValues.put(DbFields.BEARING.getName(),
                    stop.getBearing());
                parentLocationValues.put(DbFields.LOCATION_TYPE.getName(),
                    Location.TYPE_SCHEDULED_STOP);
                parentLocationValues.put(DbFields.EXACT.getName(), 1);
                parentLocationValues.put(DbFields.IS_DYNAMIC.getName(), 0);

                locationValues.add(parentLocationValues);

                if (stop.hasChildren()) {
                    for (ScheduledStop child : stop.getChildren()) {
                        existingId = codeToIdMap == null ? null : codeToIdMap
                            .get(child.getCode());
                        final int childStopId = existingId == null ? r
                            .nextInt(Integer.MAX_VALUE) : existingId;

                        final ContentValues childStopValues = new ContentValues(
                            9);
                        childStopValues.put(DbFields.ID.getName(), childStopId);
                        childStopValues.put(DbFields.PARENT_ID.getName(),
                            parentStopId);
                        childStopValues.put(DbFields.IS_PARENT.getName(), 0);
                        childStopValues.put(DbFields.STOP_TYPE.getName(), child
                            .getType() == null ? null : child.getType()
                            .toString());
                        childStopValues.put(DbFields.CELL_CODE.getName(),
                            cellCode);
                        childStopValues.put(DbFields.CODE.getName(),
                            child.getCode());
                        childStopValues.put(DbFields.SHORT_NAME.getName(),
                            child.getShortName());
                        childStopValues.put(DbFields.SERVICES.getName(),
                            child.getServices());

                        scheduledStopValues.add(childStopValues);

                        final ContentValues childLocationValues = new ContentValues(
                            9);
                        childLocationValues.put(
                            DbFields.SCHEDULED_STOP_CODE.getName(),
                            child.getCode());
                        childLocationValues.put(DbFields.NAME.getName(),
                            child.getName());
                        childLocationValues.put(DbFields.ADDRESS.getName(),
                            child.getAddress());
                        childLocationValues.put(DbFields.LAT.getName(),
                            child.getLat());
                        childLocationValues.put(DbFields.LON.getName(),
                            child.getLon());
                        childLocationValues.put(DbFields.BEARING.getName(),
                            child.getBearing());
                        childLocationValues.put(
                            DbFields.LOCATION_TYPE.getName(),
                            Location.TYPE_SCHEDULED_STOP);
                        childLocationValues.put(DbFields.EXACT.getName(), 1);
                        childLocationValues
                            .put(DbFields.IS_DYNAMIC.getName(), 0);

                        locationValues.add(childLocationValues);
                    }
                }

                // Dont need this anymore, lets save some memory..
                iter.remove();
            }
        }
    }

    private void insertInBatches(List<ContentValues> scheduledStopValues,
                                 List<ContentValues> locationValues, long sleep) {
        int counter = 0;

        boolean continueLoop = true;
        do {
            int startIndex = counter * INSERT_BATCH_SIZE;
            int endIndex = (++counter) * INSERT_BATCH_SIZE;
            if (endIndex > scheduledStopValues.size()) {
                continueLoop = false;
                endIndex = scheduledStopValues.size();
            }

            List<ContentValues> subList = scheduledStopValues.subList(
                startIndex, endIndex);
            ContentValues[] valArr = new ContentValues[subList.size()];
            subList.toArray(valArr);

            int rowCount = scheduledStopRepository.bulkInsert(valArr);

            Arrays.fill(valArr, null);
            subList = locationValues.subList(startIndex, endIndex);
            valArr = new ContentValues[subList.size()];
            subList.toArray(valArr);
            rowCount = appContext.getContentResolver().bulkInsert(
                ScheduledStopsProvider.LOCATIONS_BY_SCHEDULED_STOP_URI,
                valArr);

            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Timber.e("Error whilst sleeping for batch insert");
                }
            }
        } while (continueLoop);
    }

    //Tung: this query the SCHEDULED_STOPS table, getting out a map of <stop_code, local_id> of the stops which
    //match the cell code and and got stop_code!=null
    private Map<String, Integer> getCodeToIdMapping(final String cellCode) {
        Cursor c = null;
        Map<String, Integer> retval = new HashMap<String, Integer>();
        try {
            c = appContext.getContentResolver()
                .query(ScheduledStopsProvider.CONTENT_URI,
                    new String[]{DbFields.CODE.getName(),
                        DbFields.ID.getName()},
                    DbFields.CELL_CODE + " = ? AND " + DbFields.CODE
                        + " IS NOT NULL",
                    new String[]{cellCode}, null);
            if (c != null && c.moveToFirst()) {
                do {
                    retval.put(c.getString(0), c.getInt(1));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }

        return retval;
    }
}