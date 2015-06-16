package com.github.jtjj222.sudburytransit.models;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by Alec MacDonald on 6/15/2015.
 */
public interface PeliasService {

    @GET("/")
    void suggestLocations(@Query("q") String partialSearch, @Query("lat") String lat, @Query("lon") String lon, Callback<Places> callback);

}
