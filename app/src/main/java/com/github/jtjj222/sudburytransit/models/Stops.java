package com.github.jtjj222.sudburytransit.models;

import java.io.Serializable;
import java.util.List;

/**
 * Created by justin on 24/05/15.
 */
public class Stops implements Serializable {

    //For lists, the api returns an object with a stops key containing the list of stops
    //For a single stop, it returns an object with a stop key containing the stop
    //It is redundant for no good reason
    public List<Stop> stops;
    public Stop stop;
}
