package com.warehouse.receiving.domain;

public class Location {

    private long locationId;
    private String locationCode;
    private String locationType;

    public long getLocationId() { return locationId; }
    public void setLocationId(long locationId) { this.locationId = locationId; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
}
