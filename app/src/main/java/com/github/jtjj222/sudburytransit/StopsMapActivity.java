package com.github.jtjj222.sudburytransit;

import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;


public class StopsMapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stops_map);

        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        ArrayList<BusStopOverlayItem> items = new ArrayList<>();

        items.add(new BusStopOverlayItem("SudburyDowntown", "Downtown",
                "Description", new GeoPoint(46.491271667182488, -80.988006619736623)));
        items.add(new BusStopOverlayItem("NorthCentralPark", "North Central Park",
                "North of Central Park in New York City", new GeoPoint(46.493646595627112, -80.983546609059047)));

        BusStopOverlay overlay = new BusStopOverlay(this, items, map);

        map.getOverlays().add(overlay);

        IMapController mapController = map.getController();
        mapController.setZoom(20);
        GeoPoint startPoint = new GeoPoint(46.491271667182488, -80.988006619736623);
        mapController.setCenter(startPoint);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stops_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
