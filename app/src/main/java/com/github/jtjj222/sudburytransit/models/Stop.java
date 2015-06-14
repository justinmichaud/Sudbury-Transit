package com.github.jtjj222.sudburytransit.models;

import java.io.Serializable;
import java.util.List;

public class Stop implements Serializable {

    public int number;
    public String name;
    public double latitude, longitude;
    public List<Call> calls;
}
