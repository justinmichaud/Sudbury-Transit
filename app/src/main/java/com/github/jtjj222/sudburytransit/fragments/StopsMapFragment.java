package com.github.jtjj222.sudburytransit.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jtjj222.sudburytransit.MainActivity;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlay;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlayItem;
import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.Routes;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.github.jtjj222.sudburytransit.models.Stops;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class StopsMapFragment extends Fragment {

    public BusStopOverlay busStopOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_stops_map, parent, false);

        MapView map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        busStopOverlay = new BusStopOverlay(parent.getContext(), new ArrayList<BusStopOverlayItem>(), map, view);
        map.getOverlays().add(busStopOverlay);

        MyBus.getService(getResources().getString(R.string.mybus_api_key))
                .getStops(new Callback<Stops>() {
                    @Override
                    public void success(Stops s, Response response) {
                        final Queue<Stop> stops = new LinkedList<Stop>();
                        for (Stop stop : s.stops) {
                            // for some reason, only the numbers of the stops, and not locations
                            // are included in the /stops endpoint
                            // so we need to make a separate request for each stop
                            // or run our own api that syncs with the City's
                            stops.add(stop);
                        }

                        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                        service.scheduleAtFixedRate(new Runnable() {

                            int i = 0;

                            public void run() {

                                i++;
                                if (i > 5) {
                                    service.shutdown();
                                    return;
                                }

                                final Stop stop = stops.remove();
                                MyBus.getService(getResources().getString(R.string.mybus_api_key))
                                        .getStop(stop.number, new Callback<Stops>() {

                                            @Override
                                            public void success(Stops stops, Response response) {
                                                Stop stop = stops.stop;

                                                //possible multithreading issues
                                                BusStopOverlayItem item = new BusStopOverlayItem(stop);
                                                busStopOverlay.addItem(item);
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                MyBus.onFailure(parent.getContext(), error);
                                            }
                                        });
                            }
                        }, 0, 1, TimeUnit.SECONDS);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        MyBus.onFailure(parent.getContext(), error);
                    }
                });

        IMapController mapController = map.getController();
        mapController.setZoom(20);
        //Test start location in middle of sudbury
        GeoPoint startPoint = new GeoPoint(46.491271667182488, -80.988006619736623);
        mapController.setCenter(startPoint);

        return view;
    }

}
