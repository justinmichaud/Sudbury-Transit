package com.github.jtjj222.sudburytransit.models;

import java.util.List;

/**
 * Created by justin on 24/05/15.
 */
public class Stop {

    public int number;
    public String name;
    public double latitude, longitude;
    public List<Call> calls;
}
