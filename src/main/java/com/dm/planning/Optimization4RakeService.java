package com.dm.planning;

import com.dm.bean.Plan4RakeDetail;
import com.dm.bean.PlanOutput4RakeDetail;

import java.util.List;

public interface Optimization4RakeService<T> {
    PlanOutput4RakeDetail<T> callEngine(List<Plan4RakeDetail> planDetails);
}