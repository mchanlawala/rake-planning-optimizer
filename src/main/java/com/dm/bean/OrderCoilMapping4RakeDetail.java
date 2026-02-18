package com.dm.bean;

public class OrderCoilMapping4RakeDetail<T> {
    private int groupId;
    private T coilIndex;
    private T orderIndex;
    private double stockWeight;
    private double stockWidth;
    private double stockLength;
    private int processId;
    private String batchNo;
    private String orderNo;
    private String orderLineItem;
    private String orderScheduleNumber;
    private String location;
    private int rakeTypeId;
    private int wagonsId;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public T getCoilIndex() {
        return coilIndex;
    }

    public void setCoilIndex(T coilIndex) {
        this.coilIndex = coilIndex;
    }

    public T getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(T orderIndex) {
        this.orderIndex = orderIndex;
    }

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

    public int getWagonsId() {
        return wagonsId;
    }

    public void setWagonsId(int wagonsId) {
        this.wagonsId = wagonsId;
    }

    @Override
    public String toString() {
        return "OrderCoilMappingDetail [groupId=" + groupId + ", batchNo=" + batchNo +
                ", orderNo=" + orderNo + ", wagonsId=" + wagonsId + "]";
    }
}