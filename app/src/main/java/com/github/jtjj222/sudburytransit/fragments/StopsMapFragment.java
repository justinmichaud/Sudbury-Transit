package com.github.jtjj222.sudburytransit.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.jtjj222.sudburytransit.maps.BusStopOverlay;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlayItem;
import com.github.jtjj222.sudburytransit.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;


public class StopsMapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_stops_map, parent, false);

        MapView map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        ArrayList<BusStopOverlayItem> items = new ArrayList<>();

        //Some test data
        items.add(new BusStopOverlayItem("SudburyDowntown", "Downtown",
                "Description", new GeoPoint(46.491271667182488, -80.988006619736623)));
        items.add(new BusStopOverlayItem("NorthCentralPark", "North Central Park",
                "North of Central Park in New York City", new GeoPoint(46.493646595627112, -80.983546609059047)));

        BusStopOverlay overlay = new BusStopOverlay(parent.getContext(), items, map);

        map.getOverlays().add(overlay);

        IMapController mapController = map.getController();
        mapController.setZoom(20);
        //Test start location in middle of sudbury
        GeoPoint startPoint = new GeoPoint(46.491271667182488, -80.988006619736623);
        mapController.setCenter(startPoint);

        return view;
    }

}
