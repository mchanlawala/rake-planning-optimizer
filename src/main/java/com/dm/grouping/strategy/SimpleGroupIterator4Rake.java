package com.dm.grouping.strategy;

import com.dm.bean.*;
import com.dm.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SimpleGroupIterator4Rake implements GroupIterator4Rake {

    private final Iterator<ParameterGroup4Rake> paramGroupIterator;
    private final List<OrderWiseMaterial> materials;
    private final int planType;

    public SimpleGroupIterator4Rake(Set<ParameterGroup4Rake> paramGroups,
                                    List<OrderWiseMaterial> materials,
                                    int planType) {
        this.paramGroupIterator = paramGroups.iterator();
        this.materials = materials;
        this.planType = planType;
    }

    @Override
    public boolean hasNext() {
        return paramGroupIterator.hasNext();
    }

    @Override
    public List<Group4Rake> next() {
        ParameterGroup4Rake groupParam = paramGroupIterator.next();
        List<Group4Rake> groups = new ArrayList<>();

        List<OrderWiseMaterial> filteredMaterials = CollectionUtil.filter(materials,
                material -> groupParam.getLocation().equals(material.getLocation()));

        groups.add(new SimpleGroup4Rake(filteredMaterials, groupParam));
        return groups;
    }

    private static class SimpleGroup4Rake implements Group4Rake {
        private final List<OrderWiseMaterial> materials;
        private final ParameterGroup4Rake groupParams;

        public SimpleGroup4Rake(List<OrderWiseMaterial> materials, ParameterGroup4Rake groupParams) {
            this.materials = materials;
            this.groupParams = groupParams;
        }

        @Override
        public List<OrderWiseMaterial> getMaterials() {
            return materials;
        }

        @Override
        public ParameterGroup4Rake getGroupParams() {
            return groupParams;
        }

        @Override
        public String toString() {
            return "Group [location=" + groupParams.getLocation() +
                    ", materials=" + materials.size() + "]";
        }
    }
}