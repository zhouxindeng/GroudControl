package com.dji.sdk.sample.internal.controller;

public class LocationUpdateEvent {
    private final double latitude;
    private final double longitude;

    public LocationUpdateEvent(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
