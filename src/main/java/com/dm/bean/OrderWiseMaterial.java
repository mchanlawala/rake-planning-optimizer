package com.dm.bean;

public class OrderWiseMaterial {
    private double stockWeight;
    private double stockWidth;
    private double stockLength;
    private int noOfOrderWidth;
    private int processId;
    private String batchNo;
    private String orderNo;
    private String orderLineItem;
    private String orderScheduleNumber;
    private String location;
    private int rakeTypeId;

    public double getStockWeight() {
        return stockWeight;
    }

    public void setStockWeight(double stockWeight) {
        this.stockWeight = stockWeight;
    }

    public double getStockWidth() {
        return stockWidth;
    }

    public void setStockWidth(double stockWidth) {
        this.stockWidth = stockWidth;
    }

    public double getStockLength() {
        return stockLength;
    }

    public void setStockLength(double stockLength) {
        this.stockLength = stockLength;
    }

    public int getNoOfOrderWidth() {
        return noOfOrderWidth;
    }

    public void setNoOfOrderWidth(int noOfOrderWidth) {
        this.noOfOrderWidth = noOfOrderWidth;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderLineItem() {
        return orderLineItem;
    }

    public void setOrderLineItem(String orderLineItem) {
        this.orderLineItem = orderLineItem;
    }

    public String getOrderScheduleNumber() {
        return orderScheduleNumber;
    }

    public void setOrderScheduleNumber(String orderScheduleNumber) {
        this.orderScheduleNumber = orderScheduleNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getRakeTypeId() {
        return rakeTypeId;
    }

    public void setRakeTypeId(int rakeTypeId) {
        this.rakeTypeId = rakeTypeId;
    }

    @Override
    public String toString() {
        return "Material [stockWeight=" + stockWeight + ", stockWidth=" + stockWidth +
                ", stockLength=" + stockLength + ", batchNo=" + batchNo + ", orderNo=" + orderNo +
                ", location=" + location + "]";
    }
}