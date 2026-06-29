package org.automation.api.rest.commons.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtil {

    private final int value;
    private final TimeUnit unit;

    public TimeUtil(int value, TimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public static TimeUtil of(int value, String unit) {
        return new TimeUtil(value, TimeUnit.valueOf(unit));
    }

    public int getValue() {return value;}

    public TimeUnit getUnitDict() {return unit;}
}
