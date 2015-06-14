package com.github.jtjj222.sudburytransit.models;

import java.util.Calendar;
import java.util.Date;

public class Call {

    public String route;
    public Date passing_time;
    public Destination destination;

    public float getMinutesToPassing() {
        return (passing_time.getTime() - Calendar.getInstance().getTime().getTime()) / 1000f / 60f;
    }
}
