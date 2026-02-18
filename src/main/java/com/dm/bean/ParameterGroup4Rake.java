package com.dm.bean;

public class ParameterGroup4Rake {
    private String location;

    public ParameterGroup4Rake(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public int hashCode() {
        return 7 * (location != null ? location.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParameterGroup4Rake)) {
            return false;
        }
        ParameterGroup4Rake paramGroup = (ParameterGroup4Rake) obj;
        return location != null ? location.equalsIgnoreCase(paramGroup.location) : paramGroup.location == null;
    }

    @Override
    public String toString() {
        return "ParameterGroup [location=" + location + "]";
    }
}