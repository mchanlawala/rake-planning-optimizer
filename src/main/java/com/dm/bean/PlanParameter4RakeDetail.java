package com.dm.bean;

public class PlanParameter4RakeDetail {
    private int groupId;
    private int rakeTypeId;
    private String rakeTypeName;
    private double rakeMinWeight;
    private double rakeMaxWeight;
    private double totalCapacity;
    private int noWagons;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getRakeTypeId() {
        return rakeTypeId;
    }

    public void setRakeTypeId(int rakeTypeId) {
        this.rakeTypeId = rakeTypeId;
    }

    public String getRakeTypeName() {
        return rakeTypeName;
    }

    public void setRakeTypeName(String rakeTypeName) {
        this.rakeTypeName = rakeTypeName;
    }

    public double getRakeMinWeight() {
        return rakeMinWeight;
    }

    public void setRakeMinWeight(double rakeMinWeight) {
        this.rakeMinWeight = rakeMinWeight;
    }

    public double getRakeMaxWeight() {
        return rakeMaxWeight;
    }

    public void setRakeMaxWeight(double rakeMaxWeight) {
        this.rakeMaxWeight = rakeMaxWeight;
    }

    public double getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(double totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getNoWagons() {
        return noWagons;
    }

    public void setNoWagons(int noWagons) {
        this.noWagons = noWagons;
    }

    @Override
    public String toString() {
        return "PlanParameter4RakeDetail{" +
                "groupId=" + groupId +
                ", rakeTypeName='" + rakeTypeName + '\'' +
                '}';
    }
}