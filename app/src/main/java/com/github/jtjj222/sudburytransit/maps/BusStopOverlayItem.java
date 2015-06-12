package com.github.jtjj222.sudburytransit.maps;

/**
 * Created by justin on 17/05/15.
 */
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.github.jtjj222.sudburytransit.models.Stop;

import java.io.Serializable;

public class BusStopOverlayItem extends OverlayItem implements Serializable {

    private Stop stop;

    public BusStopOverlayItem(Stop stop) {
        super("" + stop.number, stop.name,
                "", new GeoPoint(stop.latitude, stop.longitude));
        this.stop = stop;
    }

    public Stop getStop() {
        return stop;
    }
}