package com.github.jtjj222.sudburytransit;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

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
    protected void draw(Canvas c, MapView mapView, boolean shadow) {
        super.draw(c, mapView, shadow);
    }

    protected void updatePopupView(final Context context, final BusStopOverlayItem item) {

        //TODO fix the issue with buttons staying depressed. Re-creating the view each time hides it for now
        /* if (mPopupView == null) */ mPopupView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
            .inflate(R.layout.layout_bus_stop_overlay_item_details, null);
        ((TextView) mPopupView.findViewById(R.id.txtHeading)).setText(item.getTitle());
        ((TextView) mPopupView.findViewById(R.id.txtDescription)).setText(item.getDescription());

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
        super.onDrawItem(canvas, item, curScreenCoords, aMapOrientation);

        BusStopOverlayItem focusedItem = this.getFocus();

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

            canvas.save();
            canvas.translate(popupX, popupY);
            canvas.rotate(aMapOrientation);
            mPopupView.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
        return false;
    }

}
