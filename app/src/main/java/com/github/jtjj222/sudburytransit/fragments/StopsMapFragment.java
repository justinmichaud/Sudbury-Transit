package com.github.jtjj222.sudburytransit.fragments;

import android.animation.TimeInterpolator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlay;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlayItem;
import com.github.jtjj222.sudburytransit.maps.RouteOverlay;
import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.Pelias;
import com.github.jtjj222.sudburytransit.models.Place;
import com.github.jtjj222.sudburytransit.models.Route;
import com.github.jtjj222.sudburytransit.models.SimpleDiskCache;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class StopsMapFragment extends Fragment {

    public BusStopOverlay busStopOverlay;
    public RouteOverlay routeOverlay;
    public MyLocationNewOverlay myLocationOverlay;

    public MapView map;

    private ArrayList<Stop> stops = new ArrayList<>();

    private View view = null;
    private SimpleDiskCache cache;

    private GeoCodingSearchSuggestionsHandler searchSuggestions;

    private LinearLayout searchDrawer = null;
    private boolean searchDrawerOpened = false;
    private TimeInterpolator interpolator = null; //type of animation see@developer.android.com/reference/android/animation/TimeInterpolator.html

    public boolean stopsLoaded = false;

    private SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
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
    };

    private class SearchSuggestion {
        Stop stop;
        Place place;
    }

    private class GeoCodingSearchSuggestionsHandler implements TextWatcher, AdapterView.OnItemClickListener {

        public final AutoCompleteTextView textView;
        public SearchSuggestion selectedPlace;
        public final List<SearchSuggestion> placesFound = new ArrayList<>();

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            synchronized (placesFound) {
                if (parent.getAdapter().getCount() != placesFound.size()
                        || position >= placesFound.size()) {
                    System.out.println("Invalid item click, placesFound.size is " + placesFound.size());
                    return;
                }

                selectedPlace = placesFound.get(position);
            }
        }

        public GeoCodingSearchSuggestionsHandler(AutoCompleteTextView fromText) {
            this.textView = fromText;
        }

        public void afterTextChanged(Editable s) {
            if (textView.isPerformingCompletion()) return;

            placesFound.clear();
            selectedPlace = null;

            addStopSuggestions();
            addAddressSuggestions();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private void addAddressSuggestions() {
            Pelias.getSuggestedLocations(textView.getText().toString(), new Callback<List<Place>>() {
                @Override
                public void success(List<Place> places, Response response) {
                    if (places == null || selectedPlace != null) return;

                    synchronized (placesFound) {
                        for (Place p : places) {
                            SearchSuggestion s = new SearchSuggestion();
                            s.place = p;
                            placesFound.add(s);
                        }
                    }

                    onUpdatedSuggestions();
                }

                @Override
                public void failure(RetrofitError error) {
                    Pelias.onFailure(textView.getContext(), error);
                }
            });
        }

        private void addStopSuggestions() {
            if (selectedPlace != null) return;

            synchronized (placesFound) {
                for (Stop s : stops) {
                    if ((""+s.number).contains(textView.getText().toString())
                            || s.name != null && s.name.contains(textView.getText().toString())) {
                        SearchSuggestion searchSuggestion = new SearchSuggestion();
                        searchSuggestion.stop = s;
                        placesFound.add(searchSuggestion);
                    }
                }
            }

            onUpdatedSuggestions();
        }

        private void onUpdatedSuggestions() {
            if (!StopsMapFragment.this.isVisible()) return;

            ArrayList<String> placeLocations = new ArrayList<>();

            synchronized (placesFound) {
                for (SearchSuggestion s : placesFound) {
                    if (s.place != null)
                        placeLocations.add(s.place.properties.text);
                    else if (s.stop != null)
                        placeLocations.add("Stop " + s.stop.number +
                                ((s.stop.name != null && !s.stop.name.isEmpty())? ", " + s.stop.name : ""));
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                        view.getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        placeLocations);
                textView.setAdapter(arrayAdapter);
                try {
                    textView.showDropDown();
                    //Thrown when screen is rotated, for example
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((AutoCompleteTextView) view.findViewById(R.id.searchEditText)).dismissDropDown();
    }

    // TODO add a swap button to the search.

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_stops_map, parent, false);

        ((SlidingUpPanelLayout) view).setPanelSlideListener(panelSlideListener);
        searchDrawer = (LinearLayout) view.findViewById(R.id.searchDrawer);
        interpolator = new AccelerateDecelerateInterpolator();

        //Once it has been drawn/measured at least once, we can get its height
        searchDrawer.post(new Runnable() {
            @Override
            public void run() {
                searchDrawer.setTranslationY(-searchDrawer.getHeight());
            }
        });

        setHasOptionsMenu(true);

        map = (MapView) view.findViewById(R.id.map);
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

        AutoCompleteTextView searchText = (AutoCompleteTextView) view.findViewById(R.id.searchEditText);
        searchSuggestions = new GeoCodingSearchSuggestionsHandler(searchText);
        searchText.addTextChangedListener(searchSuggestions);
        searchText.setOnItemClickListener(searchSuggestions);

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

        view.findViewById(R.id.btnGoSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchSuggestion dest = searchSuggestions.selectedPlace;

                if (dest == null) {
                    if (searchSuggestions.placesFound == null
                            || searchSuggestions.placesFound.size() < 1) return;
                    dest = searchSuggestions.placesFound.get(0);
                }

                if (dest.place != null) {
                    map.getController().animateTo(new GeoPoint(dest.place.geometry.coordinates[1],
                            dest.place.geometry.coordinates[0]));
                }
                else if (dest.stop != null) {
                    map.getController().animateTo(new GeoPoint(dest.stop.latitude,
                            dest.stop.longitude));

                    for (int i=0; i<busStopOverlay.size(); i++) {
                        if (busStopOverlay.getItem(i).getStop().number == dest.stop.number) {
                            busStopOverlay.setFocus(busStopOverlay.getItem(i));
                            break;
                        }
                    }
                }

            }
        });

        loadData(parent);

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        System.out.println("On stop");
        try {
            cache.closeAllDirs();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

                stopsLoaded = true;
                onDataLoaded();
            }

            @Override
            public void failure(RetrofitError error) {
                MyBus.onFailure(parent.getContext(), error);
            }
        });
    }

    private void onDataLoaded() {
        if (stopsLoaded) {
            view.findViewById(R.id.loading).setVisibility(View.GONE);
            System.out.println("Data loaded!");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
            case R.id.action_about:
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSearch() {
        int searchDrawerDuration = 500;

        if(searchDrawerOpened) {
            searchDrawer.animate()
                    .translationY(-searchDrawer.getHeight())
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
}
