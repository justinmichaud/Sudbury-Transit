package com.github.jtjj222.sudburytransit.models;

import android.content.Context;
import android.widget.Toast;

import com.github.jtjj222.sudburytransit.R;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by Alec MacDonald on 6/15/2015.
 */
public class Pelias {

    public static PeliasService getService() {
        return new RestAdapter.Builder()
                .setEndpoint("http://photon.komoot.de/api")
                .build()
                .create(PeliasService.class);
    }

    public static void getSuggestedLocations(String partialSearch, final Callback<ArrayList<Place>> callback) {
        getService().suggestLocations(partialSearch, "46.491271667182488", "-80.988006619736623", new Callback<Places>() {
            @Override
            public void success(Places p, Response response) {
                ArrayList<Place> places = new ArrayList<>();
                places.addAll(p.places);

                // What do we do with the places?

                callback.success(places, null);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error);
            }
        });
    }

    public static void onFailure(Context context, RetrofitError error) {
        System.out.println("Failure: " + error.toString() + ". Url: " + error.getUrl());
        Toast.makeText(context, "Could not fetch autocomplete data", Toast.LENGTH_SHORT).show();
    }
}
