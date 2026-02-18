package com.dm.bean;

import java.util.List;

public class PlanOutput4RakeDetail<T> {
    private List<OrderCoilMapping4RakeDetail<T>> orderCoilMappingDetails;

    public PlanOutput4RakeDetail(List<OrderCoilMapping4RakeDetail<T>> orderCoilMappingDetails) {
        this.orderCoilMappingDetails = orderCoilMappingDetails;
    }

    public List<OrderCoilMapping4RakeDetail<T>> getOrderCoilMappingDetails() {
        return orderCoilMappingDetails;
    }

    @Override
    public String toString() {
        return "PlanOutputDetail [orderCoilMappingDetails=" + orderCoilMappingDetails + "]";
    }
}