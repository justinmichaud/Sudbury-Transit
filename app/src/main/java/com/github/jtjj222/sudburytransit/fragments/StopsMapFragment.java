package com.github.jtjj222.sudburytransit.fragments;

import android.content.Context;
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
import com.github.jtjj222.sudburytransit.maps.RouteOverlay;
import com.github.jtjj222.sudburytransit.models.Call;
import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.Route;
import com.github.jtjj222.sudburytransit.models.Routes;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.github.jtjj222.sudburytransit.models.Stops;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class StopsMapFragment extends Fragment {

    public BusStopOverlay busStopOverlay;
    public RouteOverlay routeOverlay;
    public MyLocationNewOverlay myLocationOverlay;

    private ArrayList<Route> routes = new ArrayList<>();
    private ArrayList<Stop> stops = new ArrayList<>();

    public MapView map;

    public Stop from = null, to = null;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_stops_map, parent, false);

        map = (MapView) view.findViewById(R.id.map);
        //TODO replace with our own tiles
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        map.getController().setZoom(20);
        map.getController().setCenter(new GeoPoint(46.491271667182488, -80.988006619736623));

        routeOverlay = new RouteOverlay(parent.getContext());
        map.getOverlays().add(routeOverlay);

        busStopOverlay = new BusStopOverlay(this, parent.getContext());
        map.getOverlays().add(busStopOverlay);

        // When we get the location or we get the list of stops (whichever comes last)
        // we move the map to their location (if they haven't already selected a stop)
        myLocationOverlay = new MyLocationNewOverlay(parent.getContext(), map);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.disableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                map.getController().animateTo(myLocationOverlay
                        .getMyLocation());
                focusClosestStop(myLocationOverlay.getMyLocation());
            }
        });
        map.getOverlays().add(myLocationOverlay);

        loadRoutes(parent.getContext());
        loadStops(parent.getContext());

        return view;
    }

    private void loadStops(final Context errorContext) {
        MyBus.getService(getResources().getString(R.string.mybus_api_key))
                .getStops(new Callback<Stops>() {
                    @Override
                    public void success(Stops s, Response response) {

                        for (Stop stop : s.stops) {
                            BusStopOverlayItem item = new BusStopOverlayItem(stop);
                            busStopOverlay.addItem(item);
                            stops.add(stop);
                        }

                        focusClosestStop(myLocationOverlay.getMyLocation());
                        map.invalidate();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        MyBus.onFailure(errorContext, error);
                    }
                });
    }

    private void loadRoutes(final Context errorContext) {
        MyBus.getService(getResources().getString(R.string.mybus_api_key))
                .getRoutes(new Callback<Routes>() {
                    @Override
                    public void success(Routes r, Response response) {
                        for (Route route : r.routes) {
                            try {
                                MyBus.getService(getResources().getString(R.string.mybus_api_key))
                                        .getRoute(route.number, new Callback<Routes>() {
                                            @Override
                                            public void success(Routes routes, Response response) {
                                                StopsMapFragment.this.routes.add(routes.route);
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                MyBus.onFailure(errorContext, error);
                                            }
                                        });
                            } catch (Exception e) {e.printStackTrace();}
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        MyBus.onFailure(errorContext, error);
                    }
                });
    }

    private void focusClosestStop(GeoPoint location) {
        if (location != null && busStopOverlay.getFocus() == null) {

            BusStopOverlayItem closest = null;
            for (int i = 0; i < busStopOverlay.size(); i++) {
                BusStopOverlayItem item = busStopOverlay.getItem(i);

                if (closest == null
                        || new GeoPoint(closest.getStop().latitude,
                            closest.getStop().longitude).distanceTo(location)
                        > new GeoPoint(item.getStop().latitude, item.getStop().longitude)
                            .distanceTo(location)) {
                    closest = item;
                }
            }

            busStopOverlay.setFocus(closest);
        }
    }

    public void setNavigateTo(Stop stop) {
        this.to = stop;

        if (to != null && from != null) navigate();
    }

    public void setNavigateFrom(Stop stop) {
        this.from = stop;

        if (to != null && from != null) navigate();
    }

    // We do graph search on a digraph, where each available route
    // or transfer forms an edge from one stop to another

    protected static class RouteEdge {

        public String route;
        public Stop a, b;

        public RouteEdge(Stop a, Stop b, String route) {
            this.a = a;
            this.b = b;
            this.route = route;
        }

    }

    protected static class RouteGraph {
        private HashMap<Integer, ArrayList<RouteEdge>> adjacents = new HashMap<>();

        public void addEdge(RouteEdge e) {
            if (isAdjacent(e.a, e.b)) return;
            if (adjacents.get(e.a.number) == null) adjacents.put(e.a.number, new ArrayList<RouteEdge>());
            adjacents.get(e.a.number).add(e);
        }

        public boolean isAdjacent(Stop a, Stop b) {
            if (adjacents.get(a) == null) return false;
            return adjacents.get(a.number).contains(b);
        }

        public Collection<RouteEdge> adj(int stop) {
            return adjacents.get(stop);
        }

        public Set<Integer> vertices() {
            return adjacents.keySet();
        }
    }

    private RouteGraph buildRouteGraph() {
        RouteGraph graph = new RouteGraph();

        //Until we can get order data from the city,
        //this directed graph will be wrong
        for (Route route : routes) {
            Stop last = null;
            for (Stop stop : route.stops) {
                if (last == null) {
                    last = stop;
                    continue;
                }

                graph.addEdge(new RouteEdge(last, stop, route.number));
                last = stop;
            }
        }

        return graph;
    }

    private void navigate() {
        visualizeRouteGraph(buildRouteGraph());
    }

    private void visualizeRouteGraph(RouteGraph graph) {
        for (int stop : graph.vertices()) {
            for (RouteEdge e : graph.adj(stop)) {
                Route r = new Route();
                r.stops = new ArrayList<>();
                r.stops.add(e.a);
                r.stops.add(e.b);
                routeOverlay.routes.add(r);
            }
        }
    }

    private boolean routeContainsStop(Route route, int stop) {
        for (Stop s : route.stops) {
            if (s.number == stop) return true;
        }
        return false;
    }
}
