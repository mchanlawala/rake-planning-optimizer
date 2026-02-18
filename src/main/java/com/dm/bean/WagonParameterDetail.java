package com.dm.bean;

public class WagonParameterDetail {
    private int id;
    private String widthRangeName;
    private double widthMinRange;
    private double widthMaxRange;
    private int numberOfCoils;

    public WagonParameterDetail(int id, String widthRangeName, double widthMinRange,
                                double widthMaxRange, int numberOfCoils) {
        super();
        this.id = id;
        this.widthRangeName = widthRangeName;
        this.widthMinRange = widthMinRange;
        this.widthMaxRange = widthMaxRange;
        this.numberOfCoils = numberOfCoils;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWidthRangeName() {
        return widthRangeName;
    }

    public void setWidthRangeName(String widthRangeName) {
        this.widthRangeName = widthRangeName;
    }

    public double getWidthMinRange() {
        return widthMinRange;
    }

    public void setWidthMinRange(double widthMinRange) {
        this.widthMinRange = widthMinRange;
    }

    public double getWidthMaxRange() {
        return widthMaxRange;
    }

    public void setWidthMaxRange(double widthMaxRange) {
        this.widthMaxRange = widthMaxRange;
    }

    public int getNumberOfCoils() {
        return numberOfCoils;
    }

    public void setNumberOfCoils(int numberOfCoils) {
        this.numberOfCoils = numberOfCoils;
    }
}