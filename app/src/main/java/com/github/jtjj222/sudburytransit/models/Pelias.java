package com.github.jtjj222.sudburytransit.models;

import android.content.Context;
import android.widget.Toast;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

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

    public static void getSuggestedLocations(String partialSearch, final Callback<List<Place>> callback) {
        getService().suggestLocations(partialSearch, "46.491271667182488", "-80.988006619736623", new Callback<Features>() {
            @Override
            public void success(Features p, Response response) {
                callback.success(p.features, null);
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
