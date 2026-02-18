package com.dm;


import com.dm.bean.*;
import com.dm.grouping.strategy.SimpleGroupingStrategy4Rake;
import com.dm.planning.EnhancedHybridGASolver;
import com.dm.planning.Optimization4RakeService;
import com.dm.planning.OptimizationSolverEngine4RakesImpl;
import com.dm.util.CollectionUtil;
import com.dm.util.ExcelReader;
import com.dm.util.ExcelWriter;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class MainRakeOptimizer {

    public static void main(String[] args) {
        System.out.println("Starting Rake Planning Optimizer...");
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        GroupingStrategy4Rake strategy = new SimpleGroupingStrategy4Rake();
        final String basePath = System.getProperty("user.dir") + "/excel/";
        System.out.println("Excel path: " + basePath);

        ExcelReader excelReader = new ExcelReader();

        // Read input data
        final LinkedHashMap<Integer, LinkedHashMap<String, Object>> planningData = excelReader
                .getDataFromFile(basePath + "RakeProjectionPlanner_Input.xlsx", "Planning Parameter");

        final LinkedHashMap<Integer, LinkedHashMap<String, Object>> rakeConstraintData = excelReader
                .getDataFromFile(basePath + "RakeProjectionPlanner_Input.xlsx", "Rake Constraint");

        final LinkedHashMap<Integer, LinkedHashMap<String, Object>> wagonConstraintData = excelReader
                .getDataFromFile(basePath + "RakeProjectionPlanner_Input.xlsx", "Wagon Constraint");

        final LinkedHashMap<Integer, LinkedHashMap<String, Object>> widthRangeCombinationsData = excelReader
                .getDataFromFile(basePath + "RakeProjectionPlanner_Input.xlsx", "Width Range Combinations");

        final LinkedHashMap<Integer, LinkedHashMap<String, Object>> materialData = excelReader
                .getDataFromFile(basePath + "RakeProjectionPlanner_Input.xlsx", "Material");

        List<OrderWiseMaterial> materials = getOrderWiseMaterialData(materialData);

        // Group materials by location
        GroupIterator4Rake iterator = strategy.group(materials,
                (int) Double.parseDouble(planningData.get(2).get("SCENARIO_TYPE").toString()));

        // Use the optimized solver engine
        Optimization4RakeService service = new OptimizationSolverEngine4RakesImpl();
        //Optimization4RakeService service = new EnhancedHybridGASolver();
        List<PlanOutput4RakeDetail<Integer>> planOutputDetails = new LinkedList<>();
        CollectionUtil.each(iterator, new CollectionUtil.IIterate<List<Group4Rake>>() {
            @Override
            public void process(List<Group4Rake> groups) {
                List<Plan4RakeDetail> planParameters = new LinkedList<>();
                int groupId = 1;

                for (Group4Rake group : groups) {
                    System.out.println("Processing group: " + group);

                    List<PlanParameter4RakeDetail> parameterDetails = getPlanningParameter(
                            groupId,
                            getRakeTypeId(rakeConstraintData,
                                    (int) Double.parseDouble(planningData.get(2).get("RAKE_TYPE_ID").toString())));

                    Plan4RakeDetail planDetail = new Plan4RakeDetail(
                            planningData.get(2).get("PLAN_ID").toString(),
                            (int) Double.parseDouble(planningData.get(2).get("SCENARIO_TYPE").toString()),
                            (int) Double.parseDouble(planningData.get(2).get("RAKE_TYPE_ID").toString()));

                    planDetail.setParameterDetail(parameterDetails);
                    planDetail.setWagonParameterDetail(getWagonConstraint(wagonConstraintData));
                    planDetail.setWidthRangeCombinations(getWidthRangeCombinations(widthRangeCombinationsData));
                    planDetail.setOrderCoilMappingDetails(getMaterialMappingDetail(groupId, group.getMaterials()));
                    planParameters.add(planDetail);
                }

                planOutputDetails.add(service.callEngine(planParameters));
            }
        });
        // Write output
        ExcelWriter excelWriter = new ExcelWriter();
        excelWriter.writeWagonOutputPlan(basePath + "RakeProjectionPlanner_Output.xlsx", planOutputDetails);
        System.out.println("Output written successfully!");
        System.exit(0);
    }

    private static List<OrderCoilMapping4RakeDetail<Integer>> getMaterialMappingDetail(int groupId,
                                                                                       List<OrderWiseMaterial> materials) {
        List<OrderCoilMapping4RakeDetail<Integer>> orderCoilMappingDetails = new LinkedList<>();
        for (OrderWiseMaterial material : materials) {
            OrderCoilMapping4RakeDetail<Integer> orderCoilDetail = new OrderCoilMapping4RakeDetail<>();
            orderCoilDetail.setBatchNo(material.getBatchNo());
            orderCoilDetail.setOrderNo(material.getOrderNo());
            orderCoilDetail.setOrderLineItem(material.getOrderLineItem());
            orderCoilDetail.setStockWeight(material.getStockWeight());
            orderCoilDetail.setStockWidth(material.getStockWidth());
            orderCoilDetail.setStockLength(material.getStockLength());
            orderCoilDetail.setLocation(material.getLocation());
            orderCoilDetail.setGroupId(groupId);
            orderCoilMappingDetails.add(orderCoilDetail);
        }
        return orderCoilMappingDetails;
    }

    private static List<WagonParameterDetail> getWagonConstraint(
            LinkedHashMap<Integer, LinkedHashMap<String, Object>> wagonConstraintData) {
        List<WagonParameterDetail> wagonParameterDetails = new LinkedList<>();
        for (int key : wagonConstraintData.keySet()) {
            wagonParameterDetails.add(new WagonParameterDetail(
                    (int) Double.parseDouble(wagonConstraintData.get(key).get("ID").toString()),
                    wagonConstraintData.get(key).get("WIDTH_RANGE_NAME").toString(),
                    Double.parseDouble(wagonConstraintData.get(key).get("WIDTH_MIN_RANGE").toString()),
                    Double.parseDouble(wagonConstraintData.get(key).get("WIDTH_MAX_RANGE").toString()),
                    (int) Double.parseDouble(wagonConstraintData.get(key).get("NUMBER_OF_COIL").toString())));
        }
        return wagonParameterDetails;
    }

    private static List<WidthRangeCombinations> getWidthRangeCombinations(
            LinkedHashMap<Integer, LinkedHashMap<String, Object>> widthRangeCombinationsData) {
        List<WidthRangeCombinations> rangeCombinations = new LinkedList<>();
        for (int key : widthRangeCombinationsData.keySet()) {
            rangeCombinations.add(new WidthRangeCombinations(
                    (int) Double.parseDouble(widthRangeCombinationsData.get(key).get("ID").toString()),
                    (int) Double.parseDouble(widthRangeCombinationsData.get(key).get("NARROW").toString()),
                    (int) Double.parseDouble(widthRangeCombinationsData.get(key).get("WIDER").toString()),
                    (int) Double.parseDouble(widthRangeCombinationsData.get(key).get("EXTRA").toString()),
                    widthRangeCombinationsData.get(key).get("REMARK").toString()));
        }
        return rangeCombinations;
    }

    private static List<PlanParameter4RakeDetail> getPlanningParameter(int groupId,
                                                                       LinkedHashMap<String, Object> rakeConstraintData) {
        List<PlanParameter4RakeDetail> parameterDetails = new LinkedList<>();
        try {
            PlanParameter4RakeDetail parameterDetail = new PlanParameter4RakeDetail();
            parameterDetail.setGroupId(groupId);
            parameterDetail.setRakeTypeId((int) Double.parseDouble(rakeConstraintData.get("RAKE_TYPE_ID").toString()));
            parameterDetail.setRakeTypeName(rakeConstraintData.get("RAKE_TYPE_NAME").toString());
            parameterDetail.setRakeMinWeight(Double.parseDouble(rakeConstraintData.get("RAKE_MIN_WEIGHT").toString()));
            parameterDetail.setRakeMaxWeight(Double.parseDouble(rakeConstraintData.get("RAKE_MAX_WEIGHT").toString()));
            parameterDetail.setTotalCapacity(Double.parseDouble(rakeConstraintData.get("TOTAL_CAPACITY").toString()));
            parameterDetail.setNoWagons((int) Double.parseDouble(rakeConstraintData.get("NO_WAGONS").toString()));
            parameterDetails.add(parameterDetail);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return parameterDetails;
    }

    private static LinkedHashMap<String, Object> getRakeTypeId(
            LinkedHashMap<Integer, LinkedHashMap<String, Object>> rakeConstraintData, int rakeTypeId) {
        for (LinkedHashMap<String, Object> rakeConstraint : rakeConstraintData.values()) {
            if (rakeConstraint.containsKey("RAKE_TYPE_ID")
                    && ((int) Double.parseDouble(rakeConstraint.get("RAKE_TYPE_ID").toString()) == rakeTypeId)) {
                return rakeConstraint;
            }
        }
        return null;
    }

    private static List<OrderWiseMaterial> getOrderWiseMaterialData(
            LinkedHashMap<Integer, LinkedHashMap<String, Object>> materialData) {
        List<OrderWiseMaterial> materials = new LinkedList<>();
        for (Integer cnt : materialData.keySet()) {
            try {
                LinkedHashMap<String, Object> data = materialData.get(cnt);
                OrderWiseMaterial material = new OrderWiseMaterial();
                material.setBatchNo(data.get("BATCH_NUMBER").toString());
                material.setOrderNo(data.get("ORDER_NUMBER").toString());
                material.setOrderLineItem(data.get("ORDER_LINE_NUMBER").toString());
                material.setStockWeight(Double.parseDouble(data.get("STOCK_WEIGHT").toString()));
                material.setStockWidth(Double.parseDouble(data.get("STOCK_WIDTH").toString()));
                material.setStockLength(Double.parseDouble(data.get("STOCK_LENGTH").toString()));
                material.setLocation(data.get("LOCATION").toString());
                materials.add(material);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return materials;
    }
}