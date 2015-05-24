package com.github.jtjj222.sudburytransit.maps;

/**
 * Created by justin on 17/05/15.
 */
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class BusStopOverlayItem extends OverlayItem {

    private String description;

    public BusStopOverlayItem(String uid, String title, String description, GeoPoint location) {
        super(uid, title, description, location);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}