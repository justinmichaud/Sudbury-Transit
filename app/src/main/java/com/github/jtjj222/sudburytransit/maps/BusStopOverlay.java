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
import android.widget.ListView;
import android.widget.TextView;

import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.models.Call;
import com.github.jtjj222.sudburytransit.models.Stop;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by justin on 17/05/15.
 */
public class BusStopOverlay extends ItemizedIconOverlay<BusStopOverlayItem> implements
            ItemizedOverlay.OnFocusChangeListener {

    private Context context;
    private MapView map;

    private boolean focusChanged = false;
    private View mPopupView = null;
    private float popupX = 0, popupY = 0;

    public BusStopOverlay(Context pContext, ArrayList<BusStopOverlayItem> items, final MapView map) {
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

        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                MotionEvent localEvent = getPopupMotionEvent(motionEvent);
                if (localEvent != null) return mPopupView.dispatchTouchEvent(localEvent);
                else return false;
            }
        });

        map.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                MotionEvent localEvent = getPopupMotionEvent(motionEvent);
                if (localEvent != null) return mPopupView.dispatchGenericMotionEvent(localEvent);
                else return false;
            }
        });
    }

    //Returns an event if you need to dispach it to the popup view, or null otherwise
    private MotionEvent getPopupMotionEvent(MotionEvent canvasMotionEvent) {
        if (canvasMotionEvent == null || canvasMotionEvent.getPointerCount() != 1) return null;
        if (mPopupView != null) {

            float mx = canvasMotionEvent.getX() - popupX;
            float my = canvasMotionEvent.getY() - popupY;

            //If our touch is inside the bounds of the popup, we forward events to it
            if (mx >= 0 && my >= 0 && mx <= mPopupView.getWidth()
                    && my <= mPopupView.getHeight()) {
                MotionEvent myEvent = MotionEvent.obtain(canvasMotionEvent.getDownTime(),
                        canvasMotionEvent.getEventTime(), canvasMotionEvent.getAction(),
                        mx, my, canvasMotionEvent.getMetaState());
                mPopupView.invalidate();
                map.postInvalidate();
                return myEvent;
            }
            else return null;
        }
        else return null;
    }

    @Override
    protected boolean onTap(int index) {
        setFocus(getItem(index));
        map.invalidate();
        return true;
    }

    @Override
    public void onFocusChanged(ItemizedOverlay<?> overlay, OverlayItem newFocus) {
        focusChanged = true;
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);

        if (mPopupView != null && !shadow) {
            canvas.save();
            canvas.translate(popupX, popupY);
            canvas.rotate(mapView.getMapOrientation());
            mPopupView.draw(canvas);
            canvas.restore();
        }
    }

    protected void updatePopupView(final Context context, final BusStopOverlayItem item) {

        //TODO fix the issue with buttons staying depressed. Re-creating the view each time hides it for now
        /* if (mPopupView == null) */ mPopupView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
            .inflate(R.layout.layout_bus_stop_overlay_item_details, null);
        ((TextView) mPopupView.findViewById(R.id.txtHeading)).setText(item.getTitle());
        ((TextView) mPopupView.findViewById(R.id.txtStopNumber)).setText(""+item.getStop().number);

        ((ListView) mPopupView.findViewById(R.id.listCalls)).setAdapter(new ArrayAdapter<Call>(context, -1, item.getStop().calls) {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.layout_bus_stop_overlay_call, parent, false);

                Call call = getItem(position);
                ((TextView) convertView.findViewById(R.id.txtRouteNumber)).setText(""+call.route);
                ((TextView) convertView.findViewById(R.id.txtPassing)).setText(""
                        + (call.passing_time.getTime() - Calendar.getInstance().getTime().getTime())/1000/60 + " Minutes");
                ((TextView) convertView.findViewById(R.id.txtDestination)).setText("To "+call.destination.name);

                return convertView;
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
                map.invalidate();
            }
        });
    }

    @Override
    protected void onDrawItem(Canvas canvas, BusStopOverlayItem item, Point curScreenCoords,
                              final float aMapOrientation) {
        BusStopOverlayItem focusedItem = this.getFocus();

        //If this is the focused item, update the position to draw the overlay
        //If this is the first time since the focus changed, update the overlay too
        if (focusedItem != null && focusedItem.equals(item)) {
            if (focusChanged) {
                focusChanged = false;

                updatePopupView(context, item);

                //We manually measure it, so we can draw it to the canvas
                //The addView method shown in the examples was really buggy, especially with zooming and rotation
                //More: http://stackoverflow.com/questions/17531858/how-to-make-any-view-to-draw-to-canvas
                int widthSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.UNSPECIFIED);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.UNSPECIFIED);
                mPopupView.measure(widthSpec, heightSpec);
                mPopupView.layout(0, 0, mPopupView.getMeasuredWidth(), mPopupView.getMeasuredHeight());
            }

            this.popupX = curScreenCoords.x  - mPopupView.getWidth()/2f;
            this.popupY = curScreenCoords.y;

            item.setMarker(ContextCompat.getDrawable(context, R.drawable.ic_launcher));
        }
        else item.setMarker(getDefaultMarker(0));

        super.onDrawItem(canvas, item, curScreenCoords, aMapOrientation);
    }

    @Override
    public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
        return false;
    }

}
