package com.skedgo.tripkit.ui.utils;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.functions.Function;

public class ServiceLineOverlayTask implements Function<List<ServiceLineOverlayTask.ServiceLineInfo>, List<PolylineOptions>> {
    private static final int NON_TRAVELLED_LINE_COLOR = 0x88AAAAAA;

    public ServiceLineOverlayTask() {
    }

    @Override
    public List<PolylineOptions> apply(List<ServiceLineInfo> serviceLineInfos) {
        List<ServiceLineInfo> mLinesToDraw = serviceLineInfos;

        List<List<LineSegment>> mNonTravelledLinesToDraw = new LinkedList<List<LineSegment>>();
        List<List<LineSegment>> linesToDraw = new LinkedList<List<LineSegment>>();

        for (ServiceLineInfo line : mLinesToDraw) {

            if (line.waypoints == null || line.waypoints.isEmpty()) {
                continue;
            }

            List<LineSegment> nonTravelledLines = new ArrayList<LineSegment>();
            List<LineSegment> lineSegmentsToDraw = new ArrayList<LineSegment>();

            for (int j = 0, size = line.waypoints.size() - 1; j < size; j++) {
                final LatLng start = line.waypoints.get(j);
                final LatLng end = line.waypoints.get(j + 1);

                if (line.travelled) {
                    if (!nonTravelledLines.isEmpty()) {
                        mNonTravelledLinesToDraw.add(nonTravelledLines);
                        nonTravelledLines.clear();
                    }

                    lineSegmentsToDraw.add(new LineSegment(start, end, LineSegment.SOLID, line.color));
                } else {
                    nonTravelledLines.add(new LineSegment(start, end, LineSegment.SOLID, line.color));
                }
            }

            linesToDraw.add(lineSegmentsToDraw);

            if (!nonTravelledLines.isEmpty()) {
                mNonTravelledLinesToDraw.add(nonTravelledLines);
            }
        }

        return getPolylines(linesToDraw, mNonTravelledLinesToDraw);
    }

    private List<PolylineOptions> getPolylines(List<List<LineSegment>> mLinesToDraw, List<List<LineSegment>> mNonTravelledLinesToDraw) {
        List<PolylineOptions> polylineOptions = new ArrayList<>();
        if (!mLinesToDraw.isEmpty()) {
            List<LatLng> lines = new ArrayList<>();
            for (List<LineSegment> list : mLinesToDraw) {
                lines.clear();
                for (LineSegment line : list) {
                    lines.add(line.start);
                    lines.add(line.end);
                }

                if (!lines.isEmpty()) {
                    int color = list.get(0).color;

                    //If we have a non-black color, draw an outline!
                    if (color != Color.BLACK) {
                        polylineOptions.add(new PolylineOptions()
                            .addAll(lines)
                            .color(Color.BLACK)
                            .width(20)
                            .geodesic(true));
                    }

                    polylineOptions.add(new PolylineOptions()
                        .addAll(lines)
                        .color(color)
                        .width(color != Color.BLACK ? 12 : 14)
                        .geodesic(true));
                }
            }
        }

        if (!mNonTravelledLinesToDraw.isEmpty()) {
            List<LatLng> lines = new LinkedList<LatLng>();
            for (List<LineSegment> list : mNonTravelledLinesToDraw) {
                lines.clear();
                for (LineSegment line : list) {
                    lines.add(line.start);
                    lines.add(line.end);
                }

                if (!lines.isEmpty()) {
                    polylineOptions.add(new PolylineOptions().addAll(lines).color(NON_TRAVELLED_LINE_COLOR).width(14));
                }
            }
        }
        return polylineOptions;
    }

    public static class LineSegment {
        public static final int SOLID = 0;

        public LatLng start;
        public LatLng end;
        public int color;
        public int type;

        public LineSegment(LatLng start, LatLng end, int type, int color) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.type = type;
        }
    }

    public static class ServiceLineInfo {
        public final List<LatLng> waypoints;
        public final int color;
        public boolean travelled;

        public ServiceLineInfo(final List<LatLng> waypoints, final int color, final boolean travelled) {
            this.waypoints = waypoints;
            this.color = color;
            this.travelled = travelled;
        }
    }
}