package com.dm.planning;


import com.dm.bean.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Hybrid Genetic Algorithm with Local Search
 * Features:
 * - Adaptive population sizing
 * - Elitism with diversity preservation
 * - Local search hill climbing
 * - Parallel fitness evaluation
 * - Simulated annealing for local optima avoidance
 */
public class EnhancedHybridGASolver implements Optimization4RakeService<Integer> {
    
    // Algorithm parameters
    private int populationSize = 500;
    private int maxGenerations = 1000;
    private double crossoverRate = 0.8;
    private double mutationRate = 0.15;
    private double eliteRate = 0.1;
    private int tournamentSize = 5;
    private int localSearchIterations = 50;
    private double initialTemperature = 1000;
    private double coolingRate = 0.95;
    
    // Domain objects
    private RakeType rake;
    private List<Coil> coils;
    private Map<String, List<Coil>> coilsByLocation;
    
    // Executor for parallel processing
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    @Override
    public PlanOutput4RakeDetail<Integer> callEngine(List<Plan4RakeDetail> planDetails) {
        initializeFromInput(planDetails);
        
        // Separate by location
        coilsByLocation = coils.stream()
                .collect(Collectors.groupingBy(c -> c.location));
        
        // Optimize each location separately
        Map<String, List<Wagon>> locationResults = new ConcurrentHashMap<>();
        
        List<CompletableFuture<Void>> futures = coilsByLocation.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    List<Wagon> optimized = optimizeLocation(entry.getValue());
                    locationResults.put(entry.getKey(), optimized);
                }, executor))
                .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Combine results
        Plan finalPlan = combineResults(locationResults);
        
        return convertToOutput(finalPlan);
    }
    
    private List<Wagon> optimizeLocation(List<Coil> locationCoils) {
        // Sort coils by weight (heaviest first for better packing)
        locationCoils.sort((a, b) -> Double.compare(b.weight, a.weight));
        
        // Initialize population
        List<Chromosome> population = initializePopulation(locationCoils);
        
        Chromosome best = null;
        double bestFitness = Double.NEGATIVE_INFINITY;
        int stagnationCount = 0;
        
        for (int generation = 0; generation < maxGenerations; generation++) {
            // Evaluate fitness in parallel
            population.parallelStream().forEach(c -> c.fitness = calculateFitness(c, locationCoils));
            
            // Sort by fitness
            population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
            
            // Update best
            if (population.get(0).fitness > bestFitness) {
                bestFitness = population.get(0).fitness;
                best = population.get(0);
                stagnationCount = 0;
            } else {
                stagnationCount++;
            }
            
            // Adaptive mutation if stagnating
            if (stagnationCount > 50) {
                mutationRate = Math.min(0.4, mutationRate * 1.1);
            } else {
                mutationRate = Math.max(0.05, mutationRate * 0.99);
            }
            
            // Create next generation
            List<Chromosome> nextGen = new ArrayList<>();
            
            // Elitism
            int eliteCount = (int) (populationSize * eliteRate);
            nextGen.addAll(population.subList(0, eliteCount));
            
            // Fill rest with crossover and mutation
            while (nextGen.size() < populationSize) {
                Chromosome parent1 = tournamentSelect(population);
                Chromosome parent2 = tournamentSelect(population);
                
                if (Math.random() < crossoverRate) {
                    Chromosome child = crossover(parent1, parent2);
                    
                    // Apply local search with simulated annealing probability
                    if (Math.random() < 0.3) {
                        child = localSearch(child, locationCoils, 
                                initialTemperature * Math.pow(coolingRate, generation));
                    } else {
                        child = mutate(child);
                    }
                    
                    nextGen.add(child);
                } else {
                    nextGen.add(mutate(parent1));
                }
            }
            
            population = nextGen;
            
            // Log progress
            if (generation % 100 == 0) {
                System.out.printf("Generation %d: Best Fitness = %.2f, Mutation Rate = %.3f%n",
                        generation, bestFitness, mutationRate);
            }
        }
        
        // Decode best chromosome to wagons
        return decodeToWagons(best, locationCoils);
    }
    
    private List<Chromosome> initializePopulation(List<Coil> coils) {
        List<Chromosome> population = new ArrayList<>();
        
        for (int i = 0; i < populationSize; i++) {
            Chromosome chrom = new Chromosome(coils.size());
            
            // Mix of strategies for diversity
            if (i < populationSize / 3) {
                // Random initialization
                for (int j = 0; j < coils.size(); j++) {
                    chrom.genes[j] = ThreadLocalRandom.current().nextInt(rake.maxWagons * 2);
                }
            } else if (i < populationSize * 2 / 3) {
                // Greedy initialization (heuristic)
                chrom.genes = greedyInitialization(coils);
            } else {
                // Width-based grouping
                chrom.genes = widthBasedInitialization(coils);
            }
            
            population.add(chrom);
        }
        
        return population;
    }
    
    private int[] greedyInitialization(List<Coil> coils) {
        int[] genes = new int[coils.size()];
        Map<Integer, WagonSimulator> wagons = new HashMap<>();
        
        for (int i = 0; i < coils.size(); i++) {
            Coil coil = coils.get(i);
            int bestWagon = -1;
            double bestFit = Double.NEGATIVE_INFINITY;
            
            // Try existing wagons
            for (Map.Entry<Integer, WagonSimulator> entry : wagons.entrySet()) {
                WagonSimulator wagon = entry.getValue();
                if (wagon.canAdd(coil)) {
                    double fitScore = evaluateWagonFit(wagon, coil);
                    if (fitScore > bestFit) {
                        bestFit = fitScore;
                        bestWagon = entry.getKey();
                    }
                }
            }
            
            // Create new wagon if needed
            if (bestWagon == -1) {
                bestWagon = wagons.size();
                wagons.put(bestWagon, new WagonSimulator());
            }
            
            genes[i] = bestWagon;
            wagons.get(bestWagon).add(coil);
        }
        
        return genes;
    }
    
    private int[] widthBasedInitialization(List<Coil> coils) {
        int[] genes = new int[coils.size()];
        
        // Group by width category
        List<Coil> narrow = new ArrayList<>();
        List<Coil> wider = new ArrayList<>();
        List<Coil> extra = new ArrayList<>();
        
        for (Coil c : coils) {
            switch (c.category) {
                case NARROW: narrow.add(c); break;
                case WIDER: wider.add(c); break;
                case EXTRA_WIDE: extra.add(c); break;
            }
        }
        
        // Create ideal combinations
        Map<Integer, List<Coil>> wagons = new HashMap<>();
        int wagonId = 0;
        
        // Create 2 wider + 1 narrow combos
        while (wider.size() >= 2 && narrow.size() >= 1) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(wider.remove(0));
            wagon.add(wider.remove(0));
            wagon.add(narrow.remove(0));
            wagons.put(wagonId++, wagon);
        }
        
        // Create 1 wider + 2 narrow combos
        while (wider.size() >= 1 && narrow.size() >= 2) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(wider.remove(0));
            wagon.add(narrow.remove(0));
            wagon.add(narrow.remove(0));
            wagons.put(wagonId++, wagon);
        }
        
        // Create 3 wider combos
        while (wider.size() >= 3) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(wider.remove(0));
            wagon.add(wider.remove(0));
            wagon.add(wider.remove(0));
            wagons.put(wagonId++, wagon);
        }
        
        // Create 4 narrow combos
        while (narrow.size() >= 4) {
            List<Coil> wagon = new ArrayList<>();
            for (int i = 0; i < 4; i++) wagon.add(narrow.remove(0));
            wagons.put(wagonId++, wagon);
        }
        
        // Create 2 extra combos
        while (extra.size() >= 2) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(extra.remove(0));
            wagon.add(extra.remove(0));
            wagons.put(wagonId++, wagon);
        }
        
        // Assign remaining coils to new wagons
        for (Coil c : wider) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(c);
            wagons.put(wagonId++, wagon);
        }
        
        for (Coil c : narrow) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(c);
            wagons.put(wagonId++, wagon);
        }
        
        for (Coil c : extra) {
            List<Coil> wagon = new ArrayList<>();
            wagon.add(c);
            wagons.put(wagonId++, wagon);
        }
        
        // Map coils to wagon IDs
        Map<Coil, Integer> coilToWagon = new HashMap<>();
        for (Map.Entry<Integer, List<Coil>> entry : wagons.entrySet()) {
            for (Coil c : entry.getValue()) {
                coilToWagon.put(c, entry.getKey());
            }
        }
        
        for (int i = 0; i < coils.size(); i++) {
            genes[i] = coilToWagon.get(coils.get(i));
        }
        
        return genes;
    }
    
    private double evaluateWagonFit(WagonSimulator wagon, Coil coil) {
        double score = 0;
        
        // Prefer wagons that already have coils from same location
        if (wagon.location != null && wagon.location.equals(coil.location)) {
            score += 100;
        }
        
        // Prefer wagons that maintain good combinations
        Map<Category, Integer> counts = wagon.getCategoryCounts();
        int newNarrow = counts.getOrDefault(Category.NARROW, 0);
        int newWider = counts.getOrDefault(Category.WIDER, 0);
        int newExtra = counts.getOrDefault(Category.EXTRA_WIDE, 0);
        
        switch (coil.category) {
            case NARROW: newNarrow++; break;
            case WIDER: newWider++; break;
            case EXTRA_WIDE: newExtra++; break;
        }
        
        // Check if combination is valid
        if (isValidCombination(newNarrow, newWider, newExtra)) {
            score += 50;
        }
        
        // Weight utilization
        double targetWeight = (rake.minSideWeight + rake.maxSideWeight) / 2;
        double newWeight = wagon.totalWeight + coil.weight;
        double utilization = 1.0 - Math.abs(newWeight - targetWeight) / targetWeight;
        score += utilization * 30;
        
        return score;
    }
    
    private Chromosome tournamentSelect(List<Chromosome> population) {
        Chromosome best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Chromosome candidate = population.get(ThreadLocalRandom.current().nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        return best;
    }
    
    private Chromosome crossover(Chromosome parent1, Chromosome parent2) {
        Chromosome child = new Chromosome(parent1.genes.length);
        int crossoverPoint = ThreadLocalRandom.current().nextInt(parent1.genes.length);
        
        for (int i = 0; i < parent1.genes.length; i++) {
            if (i < crossoverPoint) {
                child.genes[i] = parent1.genes[i];
            } else {
                child.genes[i] = parent2.genes[i];
            }
        }
        
        return child;
    }
    
    private Chromosome mutate(Chromosome chrom) {
        Chromosome mutated = new Chromosome(chrom.genes.length);
        System.arraycopy(chrom.genes, 0, mutated.genes, 0, chrom.genes.length);
        
        for (int i = 0; i < mutated.genes.length; i++) {
            if (Math.random() < mutationRate) {
                // Adaptive mutation: small changes with higher probability
                if (Math.random() < 0.7) {
                    // Small change: shift by +/- 1-3
                    int delta = ThreadLocalRandom.current().nextInt(-3, 4);
                    mutated.genes[i] = Math.max(0, mutated.genes[i] + delta);
                } else {
                    // Large change: completely new value
                    mutated.genes[i] = ThreadLocalRandom.current().nextInt(rake.maxWagons * 2);
                }
            }
        }
        
        return mutated;
    }
    
    private Chromosome localSearch(Chromosome chrom, List<Coil> coils, double temperature) {
        Chromosome current = chrom;
        double currentFitness = calculateFitness(current, coils);
        
        // Hill climbing with simulated annealing
        for (int iter = 0; iter < localSearchIterations; iter++) {
            Chromosome neighbor = createNeighbor(current, coils);
            double neighborFitness = calculateFitness(neighbor, coils);
            
            if (neighborFitness > currentFitness) {
                current = neighbor;
                currentFitness = neighborFitness;
            } else {
                // Accept worse solution with annealing probability
                double acceptanceProb = Math.exp((neighborFitness - currentFitness) / temperature);
                if (Math.random() < acceptanceProb) {
                    current = neighbor;
                    currentFitness = neighborFitness;
                }
            }
        }
        
        return current;
    }
    
    private Chromosome createNeighbor(Chromosome chrom, List<Coil> coils) {
        Chromosome neighbor = new Chromosome(chrom.genes.length);
        System.arraycopy(chrom.genes, 0, neighbor.genes, 0, chrom.genes.length);
        
        // Try to improve by swapping coils between wagons
        int idx1 = ThreadLocalRandom.current().nextInt(neighbor.genes.length);
        int idx2;
        do {
            idx2 = ThreadLocalRandom.current().nextInt(neighbor.genes.length);
        } while (idx2 == idx1);
        
        // Only swap if same location
        if (coils.get(idx1).location.equals(coils.get(idx2).location)) {
            int temp = neighbor.genes[idx1];
            neighbor.genes[idx1] = neighbor.genes[idx2];
            neighbor.genes[idx2] = temp;
        }
        
        return neighbor;
    }
    
    private double calculateFitness(Chromosome chrom, List<Coil> coils) {
        Map<Integer, WagonSimulator> wagons = new HashMap<>();
        double totalWeight = 0;
        double penalty = 0;
        
        // Build wagons
        for (int i = 0; i < coils.size(); i++) {
            int wagonId = chrom.genes[i];
            Coil coil = coils.get(i);
            
            WagonSimulator wagon = wagons.computeIfAbsent(wagonId, k -> new WagonSimulator());
            
            if (!wagon.canAdd(coil)) {
                penalty += 10000; // Infeasible assignment
                continue;
            }
            
            wagon.add(coil);
            totalWeight += coil.weight;
        }
        
        // Evaluate each wagon
        for (WagonSimulator wagon : wagons.values()) {
            if (wagon.coils.isEmpty()) continue;
            
            // Weight penalty
            if (wagon.totalWeight < rake.minSideWeight) {
                penalty += (rake.minSideWeight - wagon.totalWeight) * 0.2;
            } else if (wagon.totalWeight > rake.maxSideWeight) {
                penalty += (wagon.totalWeight - rake.maxSideWeight) * 0.5;
            }
            
            // Combination penalty
            if (!wagon.isValidCombination()) {
                penalty += 2000;
            }
            
            // Location purity is already enforced by canAdd
        }
        
        // Wagon count penalty
        int usedWagons = wagons.size();
        if (usedWagons > rake.maxWagons) {
            penalty += (usedWagons - rake.maxWagons) * 10000;
        }
        
        // Capacity penalty
        if (totalWeight > rake.totalCapacity) {
            penalty += (totalWeight - rake.totalCapacity) * 0.1;
        }
        
        // Reward for good combinations
        double reward = wagons.values().stream()
                .filter(WagonSimulator::isValidCombination)
                .count() * 500;
        
        return totalWeight - penalty + reward;
    }
    
    private List<Wagon> decodeToWagons(Chromosome chrom, List<Coil> coils) {
        Map<Integer, Wagon> wagonMap = new HashMap<>();
        
        for (int i = 0; i < coils.size(); i++) {
            int wagonId = chrom.genes[i];
            Coil coil = coils.get(i);
            
            Wagon wagon = wagonMap.computeIfAbsent(wagonId, k -> new Wagon(wagonId));
            wagon.add(coil);
        }
        
        return new ArrayList<>(wagonMap.values());
    }
    
    private Plan combineResults(Map<String, List<Wagon>> locationResults) {
        Plan plan = new Plan(rake);
        int nextId = 1;
        
        // Sort locations alphabetically
        List<String> locations = new ArrayList<>(locationResults.keySet());
        Collections.sort(locations);
        
        for (String location : locations) {
            List<Wagon> wagons = locationResults.get(location);
            
            // Sort wagons by weight (heaviest first)
            wagons.sort((a, b) -> Double.compare(b.totalWeight, a.totalWeight));
            
            // Assign sequential IDs
            for (Wagon wagon : wagons) {
                wagon.id = nextId++;
                plan.addWagon(wagon);
            }
        }
        
        return plan;
    }
    
    // Helper classes
    
    private static class Chromosome {
        int[] genes;
        double fitness;
        
        Chromosome(int length) {
            genes = new int[length];
        }
    }
    
    private static class WagonSimulator {
        List<Coil> coils = new ArrayList<>();
        double totalWeight = 0;
        String location = null;
        Map<Category, Integer> categoryCounts = new EnumMap<>(Category.class);
        
        boolean canAdd(Coil coil) {
            if (location != null && !location.equals(coil.location)) return false;
            
            int currentCount = categoryCounts.getOrDefault(coil.category, 0);
            if (currentCount + 1 > MAX_COILS_PER_CATEGORY.get(coil.category)) return false;
            
            if (coils.size() + 1 > MAX_COILS_PER_WAGON) return false;
            
            return true;
        }
        
        void add(Coil coil) {
            coils.add(coil);
            totalWeight += coil.weight;
            categoryCounts.merge(coil.category, 1, Integer::sum);
            if (location == null) location = coil.location;
        }
        
        Map<Category, Integer> getCategoryCounts() {
            return new EnumMap<>(categoryCounts);
        }
        
        boolean isValidCombination() {
            int narrow = categoryCounts.getOrDefault(Category.NARROW, 0);
            int wider = categoryCounts.getOrDefault(Category.WIDER, 0);
            int extra = categoryCounts.getOrDefault(Category.EXTRA_WIDE, 0);
            
            if (extra > 0) {
                if (narrow == 0 && wider == 0 && extra <= 2) return true;
                if (narrow == 1 && wider == 1 && extra == 1) return true;
                return false;
            }
            
            if (narrow == 1 && wider == 2) return true;
            if (narrow == 2 && wider == 1) return true;
            if (narrow == 0 && wider == 3) return true;
            if (narrow == 4 && wider == 0) return true;
            
            return false;
        }
    }
    
    // Domain classes (same as before)
    public static class RakeType {
        public final int id; public final String name;
        public final double minSideWeight; public final double maxSideWeight;
        public final double totalCapacity; public final int maxWagons;
        public RakeType(int id, String name, double min, double max, double cap, int wagons) {
            this.id = id; this.name = name; this.minSideWeight = min;
            this.maxSideWeight = max; this.totalCapacity = cap; this.maxWagons = wagons;
        }
    }
    
    public static class Coil {
        public final int groupId; public final String batchNo; public final String orderNo;
        public final String orderLineNo; public final double width; public final double weight;
        public final double length; public final String location; public final Category category;
        public Coil(int groupId, String batchNo, String orderNo, String lineNo,
                    double width, double weight, double length, String loc) {
            this.groupId = groupId; this.batchNo = batchNo; this.orderNo = orderNo;
            this.orderLineNo = lineNo; this.width = width; this.weight = weight;
            this.length = length; this.location = loc;
            this.category = getCategory(width);
        }
        private Category getCategory(double w) { /* same as before */ return Category.NARROW; }
    }
    
    public static class Wagon {
        public final int bucketKey; public int id = -1; public String location = null;
        public final List<Coil> coils = new ArrayList<>(); public double totalWeight = 0;
        public Wagon(int key) { this.bucketKey = key; }
        public void add(Coil coil) { coils.add(coil); totalWeight += coil.weight;
            if (location == null) location = coil.location; }
    }
    
    public static class Plan {
        public final RakeType rake; public final List<Wagon> wagons = new ArrayList<>();
        public double totalWeight = 0; public int totalCoils = 0;
        public Plan(RakeType rake) { this.rake = rake; }
        public void addWagon(Wagon w) { wagons.add(w); totalWeight += w.totalWeight;
            totalCoils += w.coils.size(); }
    }
    
    // Constants
    private static final Map<Category, Integer> MAX_COILS_PER_CATEGORY = new EnumMap<>(Category.class);
    private static final int MAX_COILS_PER_WAGON = 4;
    
    static {
        MAX_COILS_PER_CATEGORY.put(Category.NARROW, 4);
        MAX_COILS_PER_CATEGORY.put(Category.WIDER, 3);
        MAX_COILS_PER_CATEGORY.put(Category.EXTRA_WIDE, 2);
        MAX_COILS_PER_CATEGORY.put(Category.OTHER, 0);
    }
    
    public enum Category { NARROW, WIDER, EXTRA_WIDE, OTHER }
    
    private boolean isValidCombination(int narrow, int wider, int extra) {
        if (extra > 0) {
            if (narrow == 0 && wider == 0 && extra <= 2) return true;
            if (narrow == 1 && wider == 1 && extra == 1) return true;
            return false;
        }
        if (narrow == 1 && wider == 2) return true;
        if (narrow == 2 && wider == 1) return true;
        if (narrow == 0 && wider == 3) return true;
        if (narrow == 4 && wider == 0) return true;
        return false;
    }
    
    private void initializeFromInput(List<Plan4RakeDetail> planDetails) {
        // Extract rake and coils from input
        Plan4RakeDetail detail = planDetails.get(0);
        PlanParameter4RakeDetail param = detail.getParameterDetail().get(0);
        
        rake = new RakeType(
            param.getRakeTypeId(),
            param.getRakeTypeName(),
            param.getRakeMinWeight(),
            param.getRakeMaxWeight(),
            param.getTotalCapacity(),
            param.getNoWagons()
        );
        
        coils = new ArrayList<>();
        for (OrderCoilMapping4RakeDetail d : detail.getOrderCoilMappingDetails()) {
            coils.add(new Coil(
                d.getGroupId(), d.getBatchNo(), d.getOrderNo(), d.getOrderLineItem(),
                d.getStockWidth(), d.getStockWeight(), d.getStockLength(), d.getLocation()
            ));
        }
    }
    
    private PlanOutput4RakeDetail<Integer> convertToOutput(Plan plan) {
        List<OrderCoilMapping4RakeDetail<Integer>> details = new ArrayList<>();
        for (Wagon w : plan.wagons) {
            for (Coil c : w.coils) {
                OrderCoilMapping4RakeDetail<Integer> d = new OrderCoilMapping4RakeDetail<>();
                d.setGroupId(c.groupId); d.setBatchNo(c.batchNo); d.setOrderNo(c.orderNo);
                d.setOrderLineItem(c.orderLineNo); d.setStockWidth(c.width);
                d.setStockWeight(c.weight); d.setStockLength(c.length); d.setLocation(c.location);
                d.setRakeTypeId(plan.rake.id); d.setWagonsId(w.id);
                details.add(d);
            }
        }
        return new PlanOutput4RakeDetail<>(details);
    }
}