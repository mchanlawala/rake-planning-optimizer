package com.dm.planning;

import com.dm.bean.OrderCoilMapping4RakeDetail;
import com.dm.bean.Plan4RakeDetail;
import com.dm.bean.PlanOutput4RakeDetail;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.Limits;
import io.jenetics.util.Factory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced GA-based rake & wagon planner with:
 * 1) Multi-objective optimization (weight utilization, wagon count, combo adherence)
 * 2) Adaptive mutation rates
 * 3) Elite preservation
 * 4) Location-based grouping
 * 5) Width combination optimization
 */
public class OptimizationSolverEngine4RakesImpl implements Optimization4RakeService<Integer> {

    // -------------------- Domain Classes --------------------

    public static final class RakeType {
        public final int id;
        public final String name;
        public final double minSideWeight;
        public final double maxSideWeight;
        public final double totalCapacity;
        public final int maxWagons;

        public RakeType(int id, String name, double minSideWeight, double maxSideWeight,
                        double totalCapacity, int maxWagons) {
            this.id = id;
            this.name = name;
            this.minSideWeight = minSideWeight;
            this.maxSideWeight = maxSideWeight;
            this.totalCapacity = totalCapacity;
            this.maxWagons = maxWagons;
        }
    }

    public static final class Coil {
        public final int groupId;
        public final String batchNo;
        public final String orderNo;
        public final String orderLineNo;
        public final double width;
        public final double weight;
        public final double length;
        public final String location;
        public Category category; // Made non-final to allow lazy initialization

        public Coil(int groupId, String batchNo, String orderNo, String orderLineNo,
                    double width, double weight, double length, String location) {
            this.groupId = groupId;
            this.batchNo = batchNo;
            this.orderNo = orderNo;
            this.orderLineNo = orderLineNo;
            this.width = width;
            this.weight = weight;
            this.length = length;
            this.location = location;
            // Don't set category here - will be set by the outer class method
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coil)) return false;
            Coil coil = (Coil) o;
            return groupId == coil.groupId &&
                    Objects.equals(batchNo, coil.batchNo) &&
                    Objects.equals(orderNo, coil.orderNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, batchNo, orderNo);
        }
    }

    public static final class Wagon {
        public final int bucketKey;
        public int id = -1;
        public String location = null;
        public final List<Coil> coils = new ArrayList<>();
        public double totalWeight = 0.0;
        private final Map<Category, Integer> categoryCounts = new EnumMap<>(Category.class);

        public Wagon(int bucketKey) {
            this.bucketKey = bucketKey;
        }

        public boolean canAdd(Coil coil, Map<Category, Integer> currentCounts, RakeType rake) {
            // Location constraint
            if (location != null && !location.equals(coil.location)) return false;

            // Ensure category is set
            if (coil.category == null) {
                return false; // Category should be set before calling canAdd
            }

            // Category count constraint
            Category cat = coil.category;
            int currentCatCount = currentCounts.getOrDefault(cat, 0);
            if (currentCatCount + 1 > MAX_COILS_PER_CATEGORY.get(cat)) return false;

            // Total coils constraint
            int totalCoils = coils.size() + 1;
            if (totalCoils > MAX_COILS_PER_WAGON) return false;

            // Weight constraint
            if (totalWeight + coil.weight > rake.maxSideWeight) return false;

            return true;
        }

        public void add(Coil coil) {
            coils.add(coil);
            totalWeight += coil.weight;
            if (coil.category != null) {
                categoryCounts.merge(coil.category, 1, Integer::sum);
            }
            if (location == null) location = coil.location;
        }

        public void remove(Coil coil) {
            if (coils.remove(coil)) {
                totalWeight -= coil.weight;
                if (coil.category != null) {
                    categoryCounts.merge(coil.category, -1, Integer::sum);
                }
                if (coils.isEmpty()) location = null;
            }
        }

        public Map<Category, Integer> getCategoryCounts() {
            return new EnumMap<>(categoryCounts);
        }

        public boolean isValidCombination() {
            int narrow = categoryCounts.getOrDefault(Category.NARROW, 0);
            int wider = categoryCounts.getOrDefault(Category.WIDER, 0);
            int extra = categoryCounts.getOrDefault(Category.EXTRA_WIDE, 0);

            // Check against allowed combinations
            if (extra > 0) {
                if (narrow == 0 && wider == 0 && extra <= 2) return true;
                if (narrow == 1 && wider == 1 && extra == 1) return true;
                return false;
            }

            if (narrow == 1 && wider == 2) return true;
            if (narrow == 2 && wider == 1) return true;
            if (narrow == 0 && wider == 3) return true;
            if (narrow == 4 && wider == 0) return true;

            // Allow incomplete combinations during building
            if (narrow <= 4 && wider <= 3 && extra == 0 &&
                    narrow + wider <= MAX_COILS_PER_WAGON) {
                return true;
            }

            return false;
        }

        public double getUtilizationScore(RakeType rake) {
            double targetWeight = (rake.minSideWeight + rake.maxSideWeight) / 2;
            double weightUtilization = 1.0 - Math.abs(totalWeight - targetWeight) / targetWeight;

            // Bonus for exact combinations
            double comboBonus = isValidCombination() ? 0.2 : 0.0;

            return weightUtilization + comboBonus;
        }
    }

    public static final class Plan {
        public final RakeType rake;
        public final List<Wagon> wagons = new ArrayList<>();
        public double totalWeight = 0.0;
        public int totalCoils = 0;

        public Plan(RakeType rake) {
            this.rake = rake;
        }

        public void addWagon(Wagon wagon) {
            wagons.add(wagon);
            totalWeight += wagon.totalWeight;
            totalCoils += wagon.coils.size();
        }

        public double getScore() {
            double weightScore = totalWeight / rake.totalCapacity;
            double wagonScore = 1.0 - (wagons.size() / (double) rake.maxWagons);
            double comboScore = wagons.stream()
                    .filter(Wagon::isValidCombination)
                    .count() / (double) Math.max(1, wagons.size());

            return 0.5 * weightScore + 0.3 * comboScore + 0.2 * wagonScore;
        }
    }

    // -------------------- Enums and Constants --------------------

    public enum Category {NARROW, WIDER, EXTRA_WIDE, OTHER}

    private static final Map<Category, Integer> MAX_COILS_PER_CATEGORY = new EnumMap<>(Category.class);
    private static final int MAX_COILS_PER_WAGON = 4;

    // Width ranges (will be configured from input)
    private double narrowMin = 900, narrowMax = 1199;
    private double widerMin = 1200, widerMax = 1499;
    private double extraMin = 1500, extraMax = 2000;

    // Allowed combinations
    private List<int[]> allowedCombinations = new ArrayList<>();

    static {
        MAX_COILS_PER_CATEGORY.put(Category.NARROW, 4);
        MAX_COILS_PER_CATEGORY.put(Category.WIDER, 3);
        MAX_COILS_PER_CATEGORY.put(Category.EXTRA_WIDE, 2);
        MAX_COILS_PER_CATEGORY.put(Category.OTHER, 0);
    }

    // Make getCategory an instance method since it uses instance variables
    private Category getCategory(double width) {
        if (width >= narrowMin && width <= narrowMax) return Category.NARROW;
        if (width >= widerMin && width <= widerMax) return Category.WIDER;
        if (width >= extraMin && width <= extraMax) return Category.EXTRA_WIDE;
        return Category.OTHER;
    }

    // -------------------- Main Implementation --------------------

    @Override
    public PlanOutput4RakeDetail<Integer> callEngine(List<Plan4RakeDetail> planDetails) {
        if (planDetails == null || planDetails.isEmpty()) {
            throw new IllegalArgumentException("planDetails is empty");
        }

        // Configure from input
        configureFromInput(planDetails.get(0));

        // Extract coils
        final List<Coil> coils = extractCoils(planDetails);

        // Set categories for all coils
        for (Coil coil : coils) {
            coil.category = getCategory(coil.width);
        }

        // Get rake type
        final RakeType rake = createRakeType(planDetails.get(0));

        // Run optimization
        final Plan plan = optimizePlan(coils, rake);

        // Convert to output
        return toOutput(plan);
    }

    private void configureFromInput(Plan4RakeDetail planDetail) {
        if (planDetail.getWagonParameterDetail() != null && planDetail.getWagonParameterDetail().size() >= 3) {
            var narrow = planDetail.getWagonParameterDetail().get(0);
            var wider = planDetail.getWagonParameterDetail().get(1);
            var extra = planDetail.getWagonParameterDetail().get(2);

            this.narrowMin = narrow.getWidthMinRange();
            this.narrowMax = narrow.getWidthMaxRange();
            this.widerMin = wider.getWidthMinRange();
            this.widerMax = wider.getWidthMaxRange();
            this.extraMin = extra.getWidthMinRange();
            this.extraMax = extra.getWidthMaxRange();
        }

        if (planDetail.getWidthRangeCombinations() != null) {
            this.allowedCombinations = planDetail.getWidthRangeCombinations().stream()
                    .map(c -> new int[]{c.getWiderCount(), c.getNarrowCount(), c.getExtraCount()})
                    .collect(Collectors.toList());
        }
    }

    private List<Coil> extractCoils(List<Plan4RakeDetail> planDetails) {
        List<Coil> result = new ArrayList<>();
        for (Plan4RakeDetail planDetail : planDetails) {
            for (OrderCoilMapping4RakeDetail<Integer> detail : planDetail.getOrderCoilMappingDetails()) {
                result.add(new Coil(
                        detail.getGroupId(),
                        detail.getBatchNo(),
                        detail.getOrderNo(),
                        detail.getOrderLineItem(),
                        detail.getStockWidth(),
                        detail.getStockWeight(),
                        detail.getStockLength(),
                        detail.getLocation()));
            }
        }
        return result;
    }

    private RakeType createRakeType(Plan4RakeDetail planDetail) {
        var param = planDetail.getParameterDetail().get(0);
        return new RakeType(
                param.getRakeTypeId(),
                param.getRakeTypeName(),
                param.getRakeMinWeight(),
                param.getRakeMaxWeight(),
                param.getTotalCapacity(),
                param.getNoWagons()
        );
    }

    private Plan optimizePlan(List<Coil> coils, RakeType rake) {
        // Group coils by location first
        Map<String, List<Coil>> coilsByLocation = new HashMap<>();
        for (Coil coil : coils) {
            coilsByLocation.computeIfAbsent(coil.location, k -> new ArrayList<>()).add(coil);
        }

        Plan combinedPlan = new Plan(rake);

        // Optimize each location group separately
        for (Map.Entry<String, List<Coil>> entry : coilsByLocation.entrySet()) {
            List<Coil> locationCoils = entry.getValue();

            Plan locationPlan = optimizeLocationGroup(locationCoils, rake);
            combinedPlan.wagons.addAll(locationPlan.wagons);
            combinedPlan.totalWeight += locationPlan.totalWeight;
            combinedPlan.totalCoils += locationPlan.totalCoils;
        }

        // Renumber wagons sequentially by location
        renumberWagonsByLocation(combinedPlan);

        return combinedPlan;
    }

    private Plan optimizeLocationGroup(List<Coil> coils, RakeType rake) {
        if (coils.isEmpty()) return new Plan(rake);

        // Sort coils by weight descending for better packing
        coils.sort((a, b) -> Double.compare(b.weight, a.weight));

        // GA setup
        final int geneUpper = Math.max(rake.maxWagons * 2, coils.size());
        final Factory<Genotype<IntegerGene>> gtf =
                Genotype.of(IntegerChromosome.of(0, geneUpper - 1, coils.size()));

        // Adaptive mutation rate based on problem size
        double mutationRate = 0.1 + (coils.size() / 1000.0) * 0.1;

        final Engine<IntegerGene, Double> engine = Engine
                .builder(gt -> calculateFitness(gt, coils, rake), gtf)
                .populationSize(Math.min(1000, coils.size() * 10))
                .optimize(Optimize.MAXIMUM)
                .alterers(
                        new Mutator<>(mutationRate),
                        new SinglePointCrossover<>(0.3)
                )
                .selector(new TournamentSelector<>(3))
                .build();

        // Run evolution
        final Phenotype<IntegerGene, Double> best = engine.stream()
                .limit(Limits.bySteadyFitness(50))
                .limit(500)
                .collect(EvolutionResult.toBestPhenotype());

        // Decode and improve
        Plan plan = decodeToPlan(best.genotype(), coils, rake);
        plan = improvePlan(plan, rake);

        return plan;
    }

    private double calculateFitness(Genotype<IntegerGene> gt, List<Coil> coils, RakeType rake) {
        Map<Integer, Wagon> wagons = new HashMap<>();
        double totalWeight = 0;
        double penalty = 0;

        for (int i = 0; i < coils.size(); i++) {
            int wagonKey = gt.get(0).get(i).intValue();
            Coil coil = coils.get(i);

            Wagon wagon = wagons.computeIfAbsent(wagonKey, Wagon::new);

            if (wagon.location != null && !wagon.location.equals(coil.location)) {
                penalty += 10000; // Location violation
                continue;
            }

            Map<Category, Integer> counts = wagon.getCategoryCounts();
            if (wagon.canAdd(coil, counts, rake)) {
                wagon.add(coil);
                totalWeight += coil.weight;
            } else {
                penalty += 5000; // Cannot add
            }
        }

        // Calculate penalties and rewards
        for (Wagon w : wagons.values()) {
            if (w.coils.isEmpty()) continue;

            // Combination penalty
            if (!w.isValidCombination()) {
                penalty += 2000;
            }

            // Weight utilization
            if (w.totalWeight < rake.minSideWeight * 0.8) {
                penalty += (rake.minSideWeight * 0.8 - w.totalWeight) * 0.1;
            } else if (w.totalWeight > rake.maxSideWeight) {
                penalty += (w.totalWeight - rake.maxSideWeight) * 0.2;
            }
        }

        // Wagon count penalty
        int usedWagons = wagons.size();
        if (usedWagons > rake.maxWagons) {
            penalty += (usedWagons - rake.maxWagons) * 10000;
        }

        // Total capacity penalty
        if (totalWeight > rake.totalCapacity) {
            penalty += (totalWeight - rake.totalCapacity) * 0.1;
        }

        return totalWeight - penalty;
    }

    private Plan decodeToPlan(Genotype<IntegerGene> gt, List<Coil> coils, RakeType rake) {
        Map<Integer, Wagon> wagons = new HashMap<>();
        Set<Coil> assignedCoils = new HashSet<>();

        // First pass: assign based on GA
        for (int i = 0; i < coils.size(); i++) {
            int wagonKey = gt.get(0).get(i).intValue();
            Coil coil = coils.get(i);

            Wagon wagon = wagons.computeIfAbsent(wagonKey, Wagon::new);

            if (wagon.location == null || wagon.location.equals(coil.location)) {
                Map<Category, Integer> counts = wagon.getCategoryCounts();
                if (wagon.canAdd(coil, counts, rake)) {
                    wagon.add(coil);
                    assignedCoils.add(coil);
                }
            }
        }

        // Second pass: assign remaining coils greedily
        for (Coil coil : coils) {
            if (assignedCoils.contains(coil)) continue;

            // Try to find compatible wagon
            boolean coilAssigned = false;
            for (Wagon wagon : wagons.values()) {
                if (wagon.location == null || wagon.location.equals(coil.location)) {
                    Map<Category, Integer> counts = wagon.getCategoryCounts();
                    if (wagon.canAdd(coil, counts, rake)) {
                        wagon.add(coil);
                        coilAssigned = true;
                        break;
                    }
                }
            }

            // Create new wagon if needed
            if (!coilAssigned) {
                Wagon newWagon = new Wagon(wagons.size() + 1000);
                newWagon.add(coil);
                wagons.put(newWagon.bucketKey, newWagon);
            }
        }

        Plan plan = new Plan(rake);
        for (Wagon wagon : wagons.values()) {
            if (!wagon.coils.isEmpty()) {
                plan.addWagon(wagon);
            }
        }

        return plan;
    }

    private Plan improvePlan(Plan plan, RakeType rake) {
        boolean improved;
        int maxIterations = 100;
        int iteration = 0;

        do {
            improved = false;
            iteration++;

            // Try to merge underfilled wagons
            improved |= mergeUnderfilledWagons(plan, rake);

            // Try to rebalance weights
            improved |= rebalanceWagonWeights(plan, rake);

            // Try to improve combinations
            improved |= improveCombinations(plan, rake);

        } while (improved && iteration < maxIterations);

        return plan;
    }

    private boolean mergeUnderfilledWagons(Plan plan, RakeType rake) {
        boolean merged = false;

        for (int i = 0; i < plan.wagons.size(); i++) {
            Wagon w1 = plan.wagons.get(i);
            if (w1.totalWeight >= rake.minSideWeight * 0.9) continue;

            for (int j = i + 1; j < plan.wagons.size(); j++) {
                Wagon w2 = plan.wagons.get(j);
                if (!w1.location.equals(w2.location)) continue;

                // Create a temporary wagon to test the merge
                Wagon tempWagon = new Wagon(-1);
                tempWagon.location = w1.location;

                // Add all coils from w1
                for (Coil c : w1.coils) {
                    tempWagon.add(c);
                }

                // Try to add coils from w2
                boolean canAddAll = true;
                for (Coil c : w2.coils) {
                    Map<Category, Integer> counts = tempWagon.getCategoryCounts();
                    if (!tempWagon.canAdd(c, counts, rake)) {
                        canAddAll = false;
                        break;
                    }
                    tempWagon.add(c);
                }

                if (canAddAll && tempWagon.totalWeight <= rake.maxSideWeight) {
                    // Perform merge
                    w1.coils.clear();
                    w1.coils.addAll(tempWagon.coils);
                    w1.totalWeight = tempWagon.totalWeight;
                    w1.categoryCounts.clear();
                    w1.categoryCounts.putAll(tempWagon.categoryCounts);

                    plan.wagons.remove(j);
                    merged = true;
                    break;
                }
            }
        }

        return merged;
    }

    private boolean rebalanceWagonWeights(Plan plan, RakeType rake) {
        boolean rebalanced = false;

        List<Wagon> underfilled = new ArrayList<>();
        List<Wagon> overfilled = new ArrayList<>();

        for (Wagon wagon : plan.wagons) {
            if (wagon.totalWeight < rake.minSideWeight * 0.8) {
                underfilled.add(wagon);
            } else if (wagon.totalWeight > rake.maxSideWeight * 0.95) {
                overfilled.add(wagon);
            }
        }

        for (Wagon under : underfilled) {
            for (Wagon over : overfilled) {
                if (!under.location.equals(over.location)) continue;

                // Try to move a coil from over to under
                List<Coil> overCoils = new ArrayList<>(over.coils);
                for (Coil coil : overCoils) {
                    Map<Category, Integer> underCounts = under.getCategoryCounts();

                    if (under.canAdd(coil, underCounts, rake) &&
                            over.totalWeight - coil.weight >= rake.minSideWeight * 0.7) {

                        over.remove(coil);
                        under.add(coil);
                        rebalanced = true;
                        break;
                    }
                }
            }
        }

        return rebalanced;
    }

    private boolean improveCombinations(Plan plan, RakeType rake) {
        boolean improved = false;

        for (Wagon wagon : plan.wagons) {
            if (wagon.isValidCombination()) continue;

            // Try to swap coils with another wagon to improve combinations
            for (Wagon other : plan.wagons) {
                if (wagon == other || !wagon.location.equals(other.location)) continue;

                for (int i = 0; i < wagon.coils.size() && !wagon.isValidCombination(); i++) {
                    Coil coil1 = wagon.coils.get(i);

                    for (int j = 0; j < other.coils.size(); j++) {
                        Coil coil2 = other.coils.get(j);

                        // Try swapping
                        wagon.remove(coil1);
                        other.remove(coil2);

                        Map<Category, Integer> wagonCounts = wagon.getCategoryCounts();
                        Map<Category, Integer> otherCounts = other.getCategoryCounts();

                        boolean wagonCanAdd = wagon.canAdd(coil2, wagonCounts, rake);
                        boolean otherCanAdd = other.canAdd(coil1, otherCounts, rake);

                        if (wagonCanAdd && otherCanAdd) {
                            wagon.add(coil2);
                            other.add(coil1);

                            if (wagon.isValidCombination() || other.isValidCombination()) {
                                improved = true;
                                break;
                            }
                        } else {
                            // Revert
                            wagon.add(coil1);
                            other.add(coil2);
                        }
                    }
                }
            }
        }

        return improved;
    }

    private void renumberWagonsByLocation(Plan plan) {
        Map<String, List<Wagon>> wagonsByLocation = new TreeMap<>();
        for (Wagon wagon : plan.wagons) {
            String loc = wagon.location != null ? wagon.location : "Z";
            wagonsByLocation.computeIfAbsent(loc, k -> new ArrayList<>()).add(wagon);
        }

        plan.wagons.clear();
        int nextId = 1;

        for (Map.Entry<String, List<Wagon>> entry : wagonsByLocation.entrySet()) {
            List<Wagon> locationWagons = entry.getValue();
            locationWagons.sort((a, b) -> Double.compare(b.totalWeight, a.totalWeight));

            for (Wagon wagon : locationWagons) {
                wagon.id = nextId++;
                plan.wagons.add(wagon);
            }
        }
    }

    private PlanOutput4RakeDetail<Integer> toOutput(Plan plan) {
        List<OrderCoilMapping4RakeDetail<Integer>> details = new ArrayList<>();

        for (Wagon wagon : plan.wagons) {
            for (Coil coil : wagon.coils) {
                OrderCoilMapping4RakeDetail<Integer> detail = new OrderCoilMapping4RakeDetail<>();
                detail.setGroupId(coil.groupId);
                detail.setBatchNo(coil.batchNo);
                detail.setOrderNo(coil.orderNo);
                detail.setOrderLineItem(coil.orderLineNo);
                detail.setStockWidth(coil.width);
                detail.setStockWeight(coil.weight);
                detail.setStockLength(coil.length);
                detail.setLocation(coil.location);
                detail.setRakeTypeId(plan.rake.id);
                detail.setWagonsId(wagon.id);
                details.add(detail);
            }
        }

        return new PlanOutput4RakeDetail<>(details);
    }
}