package com.github.jtjj222.sudburytransit.maps;

import com.github.jtjj222.sudburytransit.models.Stop;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

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