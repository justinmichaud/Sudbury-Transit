package com.github.jtjj222.sudburytransit.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.github.jtjj222.sudburytransit.models.Route;
import com.github.jtjj222.sudburytransit.models.Stop;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;

public class RouteOverlay extends Overlay {

    public ArrayList<Route> routes = new ArrayList<Route>();

    private Paint paint = new Paint();
    private static int[] colors = {
            Color.argb(100, 255,0,0),
            Color.argb(100, 255,255,0),
            Color.argb(100, 255,0,255),
            Color.argb(100, 0,255,0),
            Color.argb(100, 0,0, 255),
    };

    public RouteOverlay(Context pContext) {
        super(new DefaultResourceProxyImpl(pContext));
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) return;

        int color = 0;

        for (Route route : routes) {

            if (route.stops.size() == 0) continue;

            paint.reset();
            paint.setColor(colors[(color++)%colors.length]);
            paint.setStrokeWidth(10);

            Stop prev = route.stops.get(0);
            for (Stop stop : route.stops) {
                Point point = mapView.getProjection().toPixels(new GeoPoint(stop.latitude, stop.longitude), null);
                Point prevPoint = mapView.getProjection().toPixels(new GeoPoint(prev.latitude, prev.longitude), null);
                canvas.drawLine(prevPoint.x, prevPoint.y, point.x, point.y, paint);
                prev = stop;
            }
        }
    }
}
