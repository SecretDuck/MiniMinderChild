package com.tmung.miniminderchild;

public class LocationData {
    private String encryptedLocation;

    public LocationData() {
        // Default constructor required for calls to DataSnapshot.getValue(LocationData.class)
    }

    public LocationData(String encryptedLocation) {
        this.encryptedLocation = encryptedLocation;
    }

    public String getEncryptedLocation() {
        return this.encryptedLocation;
    }

    public void setEncryptedLocation(String encryptedLocation) {
        this.encryptedLocation = encryptedLocation;
    }
}


