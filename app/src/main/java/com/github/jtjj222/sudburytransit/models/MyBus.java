package com.github.jtjj222.sudburytransit.models;

import com.github.jtjj222.sudburytransit.R;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
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

}
