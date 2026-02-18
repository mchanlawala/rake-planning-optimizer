package com.dm.grouping.strategy;

import com.dm.bean.*;
import com.dm.util.CollectionUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleGroupingStrategy4Rake implements GroupingStrategy4Rake {

    @Override
    public GroupIterator4Rake group(List<OrderWiseMaterial> materials, int planType) {
        Set<ParameterGroup4Rake> paramGroup = new HashSet<>();
        paramGroup.addAll(CollectionUtil.collect(materials, material ->
                new ParameterGroup4Rake(material.getLocation())));
        return new SimpleGroupIterator4Rake(paramGroup, materials, planType);
    }
}