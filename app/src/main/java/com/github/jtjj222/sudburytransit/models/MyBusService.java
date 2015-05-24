package com.github.jtjj222.sudburytransit.models;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * An interface using retrofit to access the api found at
 * http://mybus.greatersudbury.ca/api/v2?locale=en
 */
public interface MyBusService {

    @GET("/destinations")
    void listDestinations(Callback<Destinations> callback);

    @GET("/destinations/{number}")
    void getDestination(@Path("number") int number, Callback<Destinations> callback);

    @GET("/stops")
    void getStops(Callback<Stops> callback);

    @GET("/stops/{number}")
    void getStop(@Path("number") int number, Callback<Stops> callback);

    @GET("/routes")
    void getRoutes(Callback<Routes> callback);

    @GET("/routes/{number}")
    void getRoute(@Path("number") String number, Callback<Routes> callback);
}
