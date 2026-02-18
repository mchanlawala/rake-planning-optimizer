package com.dm.bean;

import java.util.List;

public class Plan4RakeDetail {
    private String planId;
    private int scenarioType;
    private int rakeTypeId;
    private List<PlanParameter4RakeDetail> parameterDetail;
    private List<WagonParameterDetail> wagonParameterDetail;
    private List<WidthRangeCombinations> widthRangeCombinations;
    private List<OrderCoilMapping4RakeDetail<Integer>> orderCoilMappingDetails;

    public Plan4RakeDetail(String planId, int scenarioType, int rakeTypeId) {
        super();
        this.planId = planId;
        this.scenarioType = scenarioType;
        this.rakeTypeId = rakeTypeId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public int getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(int scenarioType) {
        this.scenarioType = scenarioType;
    }

    public int getRakeTypeId() {
        return rakeTypeId;
    }

    public void setRakeTypeId(int rakeTypeId) {
        this.rakeTypeId = rakeTypeId;
    }

    public List<PlanParameter4RakeDetail> getParameterDetail() {
        return parameterDetail;
    }

    public void setParameterDetail(List<PlanParameter4RakeDetail> parameterDetail) {
        this.parameterDetail = parameterDetail;
    }

    public List<WagonParameterDetail> getWagonParameterDetail() {
        return wagonParameterDetail;
    }

    public void setWagonParameterDetail(List<WagonParameterDetail> wagonParameterDetail) {
        this.wagonParameterDetail = wagonParameterDetail;
    }

    public List<OrderCoilMapping4RakeDetail<Integer>> getOrderCoilMappingDetails() {
        return orderCoilMappingDetails;
    }

    public void setOrderCoilMappingDetails(List<OrderCoilMapping4RakeDetail<Integer>> orderCoilMappingDetails) {
        this.orderCoilMappingDetails = orderCoilMappingDetails;
    }

    public List<WidthRangeCombinations> getWidthRangeCombinations() {
        return widthRangeCombinations;
    }

    public void setWidthRangeCombinations(List<WidthRangeCombinations> widthRangeCombinations) {
        this.widthRangeCombinations = widthRangeCombinations;
    }

    @Override
    public String toString() {
        return "Plan4RakeDetail{" +
                "planId='" + planId + '\'' +
                ", scenarioType=" + scenarioType +
                ", rakeTypeId=" + rakeTypeId +
                '}';
    }
}