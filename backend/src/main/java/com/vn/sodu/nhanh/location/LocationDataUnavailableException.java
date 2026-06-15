package com.vn.sodu.nhanh.location;

public class LocationDataUnavailableException extends RuntimeException {

    public LocationDataUnavailableException() {
        super("Location data is still being initialized");
    }
}
