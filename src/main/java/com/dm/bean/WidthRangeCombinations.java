package com.dm.bean;

public class WidthRangeCombinations {
    private int id;
    private int narrowCount;
    private int widerCount;
    private int extraCount;
    private String remark;

    public WidthRangeCombinations(int id, int narrowCount, int widerCount, int extraCount, String remark) {
        this.id = id;
        this.narrowCount = narrowCount;
        this.widerCount = widerCount;
        this.extraCount = extraCount;
        this.remark = remark;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNarrowCount() {
        return narrowCount;
    }

    public void setNarrowCount(int narrowCount) {
        this.narrowCount = narrowCount;
    }

    public int getWiderCount() {
        return widerCount;
    }

    public void setWiderCount(int widerCount) {
        this.widerCount = widerCount;
    }

    public int getExtraCount() {
        return extraCount;
    }

    public void setExtraCount(int extraCount) {
        this.extraCount = extraCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "WidthRangeCombinations{" +
                "id=" + id +
                ", narrow=" + narrowCount +
                ", wider=" + widerCount +
                ", extra=" + extraCount +
                '}';
    }
}