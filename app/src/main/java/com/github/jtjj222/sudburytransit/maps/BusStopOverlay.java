package com.github.jtjj222.sudburytransit.maps;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.models.Call;
import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.MyBusService;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.github.jtjj222.sudburytransit.models.Stops;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by justin on 17/05/15.
 */
public class BusStopOverlay extends ItemizedIconOverlay<BusStopOverlayItem> implements
            ItemizedOverlay.OnFocusChangeListener {

    private Context context;
    private MapView map;

    private ViewGroup mPopupView;
    private View root;

    public BusStopOverlay(Context pContext, ArrayList<BusStopOverlayItem> items, final MapView map,
                          final View root) {
        super(items, new OnItemGestureListener<BusStopOverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int index, BusStopOverlayItem item) {
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, BusStopOverlayItem item) {
                return false;
            }
        }, new DefaultResourceProxyImpl(pContext));
        setOnFocusChangeListener(this);
        this.context = pContext;
        this.map = map;
        this.root = root;
    }

    @Override
    protected boolean onTap(int index) {
        setFocus(getItem(index));
        map.invalidate();
        return true;
    }

    @Override
    public void onFocusChanged(ItemizedOverlay<?> overlay, OverlayItem newFocus) {

        for (int i=0; i<size(); i++) {
            OverlayItem item = getItem(i);

            //TODO replace with non-selected marker
            item.setMarker(getDefaultMarker(0));
        }

        if (newFocus == null || !(newFocus instanceof BusStopOverlayItem)) {
            updatePopupView(null);
        }
        else {
            //TODO replace with selected marker
            newFocus.setMarker(ContextCompat.getDrawable(context, R.drawable.ic_launcher));
            updatePopupView((BusStopOverlayItem) newFocus);
        }
        map.invalidate();
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);
    }

    protected void updatePopupView(final BusStopOverlayItem item) {

        if (mPopupView == null) {
            mPopupView = (ViewGroup) ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.layout_bus_stop_overlay_item_details, null);

            ((ViewGroup) root.findViewById(R.id.slide_up)).addView(mPopupView);
        }

        ((TextView) mPopupView.findViewById(R.id.txtHeading)).setText(item.getTitle());
        ((TextView) mPopupView.findViewById(R.id.txtStopNumber)).setText("" + item.getStop().number);

        MyBus.getService(context.getResources().getString(R.string.mybus_api_key))
                .getStop(item.getStop().number, new Callback<Stops>() {
                    @Override
                    public void success(Stops stops, Response response) {
                        ((ListView) mPopupView.findViewById(R.id.listCalls))
                                .setAdapter(new ArrayAdapter<Call>(context, -1, stops.stop.calls) {
                            public View getView(int position, View convertView, ViewGroup parent) {
                                if (convertView == null)
                                    convertView = LayoutInflater.from(getContext())
                                            .inflate(R.layout.layout_bus_stop_overlay_call, parent, false);

                                Call call = getItem(position);
                                ((TextView) convertView.findViewById(R.id.txtRouteNumber)).setText("" + call.route);
                                ((TextView) convertView.findViewById(R.id.txtPassing)).setText(""
                                        + (call.passing_time.getTime() - Calendar.getInstance().getTime().getTime()) / 1000 / 60 + " Minutes");
                                ((TextView) convertView.findViewById(R.id.txtDestination)).setText("To " + call.destination.name);

                                return convertView;
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        MyBus.onFailure(context, error);
                    }
                });

        mPopupView.findViewById(R.id.btnViewBusses).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle("You clicked it")
                        .setMessage("You clicked view stop: " + item.getTitle())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        mPopupView.findViewById(R.id.btnClosePopup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFocus(null);
                mPopupView = null;
            }
        });
    }

    @Override
    protected void onDrawItem(Canvas canvas, BusStopOverlayItem item, Point curScreenCoords,
                              final float aMapOrientation) {
        super.onDrawItem(canvas, item, curScreenCoords, aMapOrientation);
    }

    @Override
    public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
        return false;
    }

}
