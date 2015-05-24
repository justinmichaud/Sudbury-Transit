package com.github.jtjj222.sudburytransit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jtjj222.sudburytransit.models.MyBus;
import com.github.jtjj222.sudburytransit.models.MyBusService;
import com.github.jtjj222.sudburytransit.models.Routes;
import com.github.jtjj222.sudburytransit.models.Stop;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Placeholder for now, plan to replace this activity with a
        // material design nav drawer
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.container, new StopsMapFragment())
//                .commit();

        //Example of how to use api
        MyBus.getService(getResources().getString(R.string.mybus_api_key))
                .getRoute("002", new Callback<Routes>() {
                    @Override
                    public void success(Routes routes, Response response) {
                        TextView tv = new TextView(MainActivity.this);
                        tv.setText(tv.getText() + "\r\n" + routes.route.number + ": \r\n");
                        for (Stop stop : routes.route.stops) {
                            tv.setText(tv.getText() + "-" + stop.name + " at " + stop.latitude + "\r\n");
                        }
                        ((LinearLayout) findViewById(R.id.container)).addView(tv);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        System.out.println("Failure: " + error.toString() + ". Url: " + error.getUrl());
                        Toast.makeText(MainActivity.this, "Could not fetch realtime data", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
