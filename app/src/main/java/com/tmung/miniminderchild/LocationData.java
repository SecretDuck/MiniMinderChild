package com.tmung.miniminderchild;

/*
public class LocationData {
    private double latitude;
    private double longitude;
    private String id;

    // Needed for Firebase's automatic data mapping
    public LocationData() { }

    public LocationData(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
 */
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


