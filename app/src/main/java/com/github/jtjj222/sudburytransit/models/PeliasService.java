package com.github.jtjj222.sudburytransit.models;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by Alec MacDonald on 6/15/2015.
 */
public interface PeliasService {

    @GET("/api?q={partialSearch}&lat=46.491271667182488&lon=80.988006619736623")
    void suggestLocations(@Path("partialSearch") String partialSearch, Callback<Places> callback);

}
