package com.github.jtjj222.sudburytransit.models;

import android.content.Context;
import android.widget.Toast;

import com.github.jtjj222.sudburytransit.R;
import com.github.jtjj222.sudburytransit.maps.BusStopOverlayItem;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by justin on 24/05/15.
 */
public class MyBus {

    public static MyBusService getService(final String api_key) {
        return new RestAdapter.Builder()
                .setEndpoint("http://mybus.greatersudbury.ca/api/v2")
                //Add our auth token
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam("auth_token", api_key);
                        System.out.println("Making request");
                    }
                })
                //Parse dates properly
                .setConverter(new GsonConverter(new GsonBuilder()
                        .registerTypeAdapter(Date.class, new TypeAdapter<Date>() {
                            @Override
                            public void write(JsonWriter out, Date value) throws IOException {
                                throw new RuntimeException("Not Implemented");
                            }

                            @Override
                            public Date read(JsonReader in) throws IOException {
                                try {
                                    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(in.nextString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                        })
                        .create()))
                .build()
                .create(MyBusService.class);
    }

    public static void onFailure(Context context, RetrofitError error) {
        System.out.println("Failure: " + error.toString() + ". Url: " + error.getUrl());
        Toast.makeText(context, "Could not fetch realtime data", Toast.LENGTH_SHORT).show();
    }

    private static void requestStops(final Context context, final SimpleDiskCache cache,
                                     final Callback<ArrayList<Stop>> callback) {

        getService(context.getResources().getString(R.string.mybus_api_key))
                .getStops(new Callback<Stops>() {
                    @Override
                    public void success(Stops s, Response response) {
                        ArrayList<Stop> stops = new ArrayList<Stop>();
                        stops.addAll(s.stops);

                        writeToCache(cache, "stops", stops);

                        callback.success(stops, null);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        callback.failure(error);
                    }
                });
    }

    private static void requestRoutes(final Context context, final SimpleDiskCache cache,
                            final Callback<ArrayList<Route>> callback) {

        MyBus.getService(context.getResources().getString(R.string.mybus_api_key))
                .getRoutes(new Callback<Routes>() {
                    @Override
                    public void success(final Routes r, Response response) {
                        final List<Object> threadsafeList = Collections.synchronizedList(new ArrayList<>());

                        for (Route route : r.routes) {
                            try {
                                getService(context.getResources().getString(R.string.mybus_api_key))
                                        .getRoute(route.number, new Callback<Routes>() {
                                            @Override
                                            public void success(Routes routes, Response response) {
                                                threadsafeList.add(routes.route);

                                                if (threadsafeList.size() == r.routes.size()) {
                                                    ArrayList<Route> realRoutes = new ArrayList<>();
                                                    for (Object o : threadsafeList)
                                                        realRoutes.add((Route) o);
                                                    writeToCache(cache, "routes", realRoutes);
                                                    callback.success(realRoutes, null);
                                                }
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                callback.failure(error);
                                            }
                                        });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        callback.failure(error);
                    }
                });
    }

    private static void writeToCache(SimpleDiskCache cache, String key, Object value) {

        if (cache == null) return;
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    cache.openStream(key, new HashMap<String, Serializable>()));
            oos.writeObject(value);
            System.out.println("Wrote " + key + " to cache.");
            oos.close();

        } catch (IOException e) {
            e.printStackTrace();
            try {
                oos.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static Object loadFromCache(SimpleDiskCache cache, String key) {
        if (cache == null) return null;

        try (ObjectInputStream is =
                     new ObjectInputStream(cache.getInputStream(key).getInputStream())){

            Object o = is.readObject();
            System.out.println("Loaded " + key + " from cache");
            return o;
        } catch (IOException|ClassNotFoundException|NullPointerException e) {
            return null;
        }
    }

    private static void invalidateCacheIfTooOld(SimpleDiskCache cache) {
        try {
            Object time = loadFromCache(cache, "time");
            if (time == null || !(time instanceof Long)) {
                cache.clear();
                writeToCache(cache, "time", System.currentTimeMillis());
                System.out.println("Invalidated cache");
                return;
            }

            long diff = System.currentTimeMillis() - (Long) time;
            if (diff > 604800000) {
                cache.clear();
                writeToCache(cache, "time", System.currentTimeMillis());
                System.out.println("Invalidated cache");
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadStops(final Context context, final SimpleDiskCache cache,
                                 final Callback<ArrayList<Stop>> callback) {
        invalidateCacheIfTooOld(cache);

        Object o = loadFromCache(cache, "stops");
        if (o == null || !(o instanceof List)) {
            requestStops(context, cache, callback);
        }
        else {
            ArrayList<Stop> stops = new ArrayList<>();
            stops.addAll((List<Stop>) o);
            callback.success(stops, null);
        }
    }

    public static void loadRoutes(final Context context, final SimpleDiskCache cache,
                                 final Callback<ArrayList<Route>> callback) {
        invalidateCacheIfTooOld(cache);

        Object o = loadFromCache(cache, "routes");
        if (o == null || !(o instanceof List)) {
            requestRoutes(context, cache, callback);
        }
        else {
            ArrayList<Route> routes = new ArrayList<>();
            routes.addAll((List<Route>) o);
            callback.success(routes, null);
        }
    }

}
