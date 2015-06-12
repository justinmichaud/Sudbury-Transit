package com.github.jtjj222.sudburytransit.maps;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.fragments.StopsMapFragment;
import com.github.jtjj222.sudburytransit.models.Call;
import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.MyBusService;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.github.jtjj222.sudburytransit.models.Stops;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by justin on 17/05/15.
 */
public class BusStopOverlay extends ItemizedIconOverlay<BusStopOverlayItem> implements
        ItemizedOverlay.OnFocusChangeListener, Serializable {

    private StopsMapFragment fragment;
    private ViewGroup mPopupView;
    private Context context;

    private BitmapDrawable bus_icon, bus_icon_selected;

    public BusStopOverlay(StopsMapFragment fragment, Context context) {
        super(new ArrayList<BusStopOverlayItem>(), null,
                new DefaultResourceProxyImpl(context));
        setOnFocusChangeListener(this);
        this.fragment = fragment;
        this.context = context;

        this.bus_icon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_icon);
        this.bus_icon_selected = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_icon_selected);

        //Scale based on screen resolution
        this.bus_icon = new BitmapDrawable(context.getResources(),
                Bitmap.createScaledBitmap(bus_icon.getBitmap(), getPixelsFromDP(40),
                        getPixelsFromDP(48) , false));
        this.bus_icon_selected = new BitmapDrawable(context.getResources(),
                Bitmap.createScaledBitmap(bus_icon_selected.getBitmap(), getPixelsFromDP(40),
                        getPixelsFromDP(48) , false));
    }

    @Override
    protected boolean onTap(int index) {
        setFocus(getItem(index));
        fragment.map.invalidate();
        return true;
    }

    @Override
    public void onFocusChanged(ItemizedOverlay<?> overlay, OverlayItem newFocus) {

        if (newFocus == null || !(newFocus instanceof BusStopOverlayItem)) {
            updatePopupView(null);
        }
        else {
            updatePopupView((BusStopOverlayItem) newFocus);
        }
        fragment.map.invalidate();
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);
    }

    protected void updatePopupView(final BusStopOverlayItem item) {

        if (item == null) return;

        if (mPopupView == null) {
            mPopupView = (ViewGroup) ((LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.layout_bus_stop_overlay_item_details, null);

            ((ViewGroup) fragment.getView().findViewById(R.id.slide_up)).addView(mPopupView);
        }

        ((TextView) mPopupView.findViewById(R.id.txtHeading)).setText(item.getTitle());
        ((TextView) mPopupView.findViewById(R.id.txtStopNumber)).setText("Stop " + item.getStop().number);

        ((ListView) mPopupView.findViewById(R.id.listCalls)).setAdapter(null);

        mPopupView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

        MyBus.getService(fragment.getView().getResources().getString(R.string.mybus_api_key)).getStop(item.getStop().number, new Callback<Stops>() {
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
                                        + (int) call.getMinutesToPassing() + " Minutes");
                                ((TextView) convertView.findViewById(R.id.txtDestination)).setText("To " + call.destination.name);

                                mPopupView.findViewById(R.id.progress_bar).setVisibility(View.GONE);

                                return convertView;
                            }
                        });
            }

            @Override
            public void failure(RetrofitError error) {
                MyBus.onFailure(context, error);
                mPopupView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
            }
        });

        mPopupView.findViewById(R.id.btnNavigateFrom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.setNavigateFrom(getFocus().getStop());
            }
        });

        mPopupView.findViewById(R.id.btnNavigateTo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.setNavigateTo(getFocus().getStop());
            }
        });

        mPopupView.findViewById(R.id.btnClosePopup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFocus(null);
                animateSlideUpClose();
            }
        });

        animateSlideUpOpen();
    }

    protected void animateSlideUpOpen() {
        animateSlideUp(((SlidingUpPanelLayout) fragment.getView()).getPanelHeight(),
                getPixelsFromDP(150));
    }

    protected void animateSlideUpClose() {
        ((SlidingUpPanelLayout) fragment.getView()).setPanelHeight(0);
        ((SlidingUpPanelLayout) fragment.getView()).setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private int getPixelsFromDP(int dp) {
        return (int) (dp * (context.getResources().getDisplayMetrics().densityDpi / 160f));
    }

    protected void animateSlideUp(final int from, final int to) {

        Animation anim = new Animation() {
            //http://stackoverflow.com/questions/22616605/umano-android-slidinguppanel-animation
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float height = (to - from) * interpolatedTime + from;

                ((SlidingUpPanelLayout) fragment.getView()).setPanelHeight((int) height);
                fragment.getView().requestLayout();
            }
        };
        anim.setDuration(250);
        fragment.getView().startAnimation(anim);
    }

    @Override
    protected void onDrawItem(Canvas canvas, BusStopOverlayItem item, Point curScreenCoords,
                              final float aMapOrientation) {
        if (!canvas.getClipBounds().contains(curScreenCoords.x, curScreenCoords.y)) return;

        if (getFocus() != null && getFocus().equals(item)) {
            item.setMarker(bus_icon_selected);
        }
        else {
            item.setMarker(bus_icon);
        }

        super.onDrawItem(canvas, item, curScreenCoords, aMapOrientation);
    }

    @Override
    public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
        return false;
    }

}
