package com.github.jtjj222.sudburytransit.fragments;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlay;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlayItem;
import com.github.jtjj222.sudburytransit.maps.RouteOverlay;
import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.Route;
import com.github.jtjj222.sudburytransit.models.Routes;
import com.github.jtjj222.sudburytransit.models.SimpleDiskCache;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.github.jtjj222.sudburytransit.models.Stops;
import com.jakewharton.disklrucache.DiskLruCache;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class StopsMapFragment extends Fragment {

    public BusStopOverlay busStopOverlay;
    public RouteOverlay routeOverlay;
    public MyLocationNewOverlay myLocationOverlay;

    private ArrayList<Route> routes = new ArrayList<>();
    private ArrayList<Stop> stops = new ArrayList<>();
    private SimpleDiskCache cache;

    public MapView map;

    private LinearLayout searchDrawer = null;
    private int searchDrawerHeight; // height of the FrameLayout (generated automatically)
    private boolean searchDrawerOpened = false;
    private int searchDrawerDuration = 500; //time in milliseconds
    private TimeInterpolator interpolator = null; //type of animation see@developer.android.com/reference/android/animation/TimeInterpolator.html

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_stops_map, parent, false);

        searchDrawer = (LinearLayout) view.findViewById(R.id.searchDrawer);
        interpolator = new AccelerateDecelerateInterpolator();

        searchDrawer.post(new Runnable() {
            @Override
            public void run() {
                searchDrawerHeight = searchDrawer.getHeight();
                searchDrawer.setTranslationY(-searchDrawerHeight);
            }
        });

        setHasOptionsMenu(true);

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

        view.findViewById(R.id.tglStops).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((ToggleButton) view).isChecked()) busStopOverlay.setEnabled(true);
                else busStopOverlay.setEnabled(false);
                map.invalidate();
            }
        });

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

        loadData(parent);

        ((SlidingUpPanelLayout) view).setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            double lastOffset = 1.0;

            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (slideOffset < lastOffset) {
                    ((SlidingUpPanelLayout) view).setPanelHeight(0);
                    ((SlidingUpPanelLayout) view).setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    busStopOverlay.setFocus(null);
                }
                lastOffset = slideOffset;
            }

            @Override
            public void onPanelExpanded(View panel) {
            }

            @Override
            public void onPanelCollapsed(View panel) {
            }

            @Override
            public void onPanelAnchored(View panel) {
            }

            @Override
            public void onPanelHidden(View panel) {
            }
        });

        return view;
    }

    private void loadData(final View parent) {
        try {
            cache = SimpleDiskCache.open(parent.getContext().getCacheDir(), 1, 1048576);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MyBus.loadStops(parent.getContext(), cache, new Callback<ArrayList<Stop>>() {
            @Override
            public void success(ArrayList<Stop> stops, Response response) {
                StopsMapFragment.this.stops = stops;
                for (Stop s : stops) busStopOverlay.addItem(new BusStopOverlayItem(s));
                focusClosestStop(myLocationOverlay.getMyLocation());
                map.invalidate();
            }

            @Override
            public void failure(RetrofitError error) {
                MyBus.onFailure(parent.getContext(), error);
            }
        });

        MyBus.loadRoutes(parent.getContext(), cache, new Callback<ArrayList<Route>>() {
            @Override
            public void success(ArrayList<Route> routes, Response response) {
                StopsMapFragment.this.routes = routes;
            }

            @Override
            public void failure(RetrofitError error) {
                MyBus.onFailure(parent.getContext(), error);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                System.out.println("Search test");
                openSearch();
                return true;
            case R.id.action_settings:
                System.out.println("Settings test");
                // openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSearch() {
        if(searchDrawerOpened) {
            searchDrawer.animate()
                    .translationY(-searchDrawerHeight)
                    .setDuration(searchDrawerDuration)
                    .setInterpolator(interpolator)
                    .start();
            searchDrawerOpened = false;
        } else {
            searchDrawer.animate()
                    .translationY(0)
                    .setDuration(searchDrawerDuration)
                    .setInterpolator(interpolator)
                    .start();
            searchDrawerOpened = true;
        }
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

    // We do k shortest path graph search on a digraph,
    // where each available route forms an edge from one stop to another
    // This is done so that it can recommend transfers at places other
    // than the transit terminal

    // When we get timing data from the city, timing will be used in the weights
    // The graph also contains nodes for the from and to locations, with
    // the distance being the weight

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
            if (!adjacents.containsKey(stop)) return new ArrayList<>();
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

            if (route.stops.size() == 0) continue;

            Stop last = null;
            for (Stop stop : route.stops) {
                if (last == null) {
                    last = stop;
                    continue;
                }

                graph.addEdge(new RouteEdge(last, stop, route.number));
                last = stop;
            }

            graph.addEdge(new RouteEdge(last, route.stops.get(0), route.number));
        }

        return graph;
    }

    //K shortest path algorithm
    private void navigate(GeoPoint from, GeoPoint to) {

//        int K = 5; //TODO load from preference; Max number of routes to find
//
//        PriorityQueue<RouteEdge[]> routes = new PriorityQueue<>(10, new Comparator<RouteEdge[]>() {
//            @Override
//            public int compare(RouteEdge[] a, RouteEdge[] b) {
//                int transfersA = 0, transfersB = 0;
//
//                for (int i=1; i<a.length; i++) if (!a[i-1].route.equals(a[i].route))  transfersA++;
//                for (int i=1; i<b.length; i++) if (!b[i-1].route.equals(b[i].route))  transfersB++;
//
//                return transfersA - transfersB;
//            }
//        });
//
//        HashMap<Integer, Integer> seen = new HashMap<>();
//        LinkedList<RouteEdge[]> pathsFound = new LinkedList<>();
//        RouteGraph graph = buildRouteGraph();
//
//        for (Stop s : stops) {seen.put(s.number, 0);}
//
//        for (RouteEdge e : graph.adj(from.number)) {
//            routes.add(new RouteEdge[]{e});
//        }
//        seen.put(from.number, 1);
//
//        while (!routes.isEmpty() && seen.get(to.number) < K) {
//            RouteEdge[] route = routes.remove();
//
//            int b = route[route.length-1].b.number;
//            if (seen.get(b) >= K) continue;
//            seen.put(b, seen.get(b) + 1);
//
//            if (b == to.number) {
//                pathsFound.add(route);
//                continue;
//            }
//
//            for (RouteEdge e : graph.adj(b)) {
//                routes.add(push_copy(e, route));
//            }
//        }
//
//        //Remove paths that take more than two transfers
//        Iterator<RouteEdge[]> itr = pathsFound.iterator();
//        while (itr.hasNext()) {
//            RouteEdge[] path = itr.next();
//
//            int transfers = 0;
//            for (int i=1; i<path.length; i++)
//                if (!path[i-1].route.equals(path[i].route)) transfers++;
//
//            if (transfers > 2) {
//                itr.remove();
//                System.out.println("Info: Removed path with more than two transfers");
//            }
//        }
//
//        System.out.println(pathsFound.size() + " paths found.");
//        for (RouteEdge[] path : pathsFound) {
//            System.out.println("Path: ");
//
//            System.out.println("Get on bus " + path[0].route
//                    + " at stop " + path[0].a.number + " "
//                    + path[0].a.name + ".");
//
//            for (int i=1; i<path.length-1; i++) {
//                if (!path[i].route.equals(path[i-1].route)) {
//                    System.out.println("Get off at stop " + path[i].a.number + " "
//                            + path[i].a.name + ".");
//                    System.out.println("Get on bus " + path[i].route + " at this stop.");
//                }
//            }
//
//            System.out.println("Get off at stop " + path[path.length-1].a.number + " "
//                    + path[path.length-1].a.name + ".");
//        }
//
//        visualizePaths(pathsFound);
    }

    //Debug method
    private void visualizeRouteGraph(RouteGraph graph) {
        routeOverlay.routes.clear();

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

    //Debug method
    private void visualizePaths(List<RouteEdge[]> paths) {
        routeOverlay.routes.clear();

        for (RouteEdge[] path : paths) {

            Route r = new Route();
            r.stops = new ArrayList<>();

            for (int i=0; i<path.length; i++) {
                if (i == 0) r.stops.add(path[i].a);
                else r.stops.add(path[i].b);
            }

            routeOverlay.routes.add(r);
        }
    }

    private boolean containsStop(int stop, RouteEdge... haystack) {
        for (RouteEdge e : haystack) {
            if (e.a.number == stop || e.b.number==stop) return true;
        }
        return false;
    }

    private <T> T[] push_copy(T tl, T... list) {
        T[] n = Arrays.copyOf(list, list.length+1);
        n[n.length-1] = tl;
        return n;
    }

    private Stop getStop(int number) {
        for (Stop s : stops) {
            if (s.number == number) return s;
        }
        return null;
    }

    private Route getRoute(String number) {
        for (Route r : routes) {
            if (r.number.equals(number)) return r;
        }
        return null;
    }
}
