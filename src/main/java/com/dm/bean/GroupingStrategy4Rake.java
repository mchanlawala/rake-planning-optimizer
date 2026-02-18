package com.dm.bean;

import java.util.List;

public interface GroupingStrategy4Rake {
    GroupIterator4Rake group(List<OrderWiseMaterial> materials, int planType);
}