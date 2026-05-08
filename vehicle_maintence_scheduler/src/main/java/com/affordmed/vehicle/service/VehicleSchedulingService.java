package com.affordmed.vehicle.service;

import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.vehicle.domain.Depot;
import com.affordmed.vehicle.domain.OptimizeAllResponse;
import com.affordmed.vehicle.domain.OptimizeResponse;
import com.affordmed.vehicle.domain.OptimizationResult;
import com.affordmed.vehicle.domain.VehicleTask;
import com.affordmed.vehicle.handler.ResourceNotFoundException;
import com.affordmed.vehicle.repository.DepotRepository;
import com.affordmed.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleSchedulingService {

    private final DepotRepository depotRepository;
    private final VehicleRepository vehicleRepository;
    private final KnapsackOptimizer knapsackOptimizer;
    private final LoggingMiddleware loggingMiddleware;

    public VehicleSchedulingService(DepotRepository depotRepository,
                                    VehicleRepository vehicleRepository,
                                    KnapsackOptimizer knapsackOptimizer,
                                    LoggingMiddleware loggingMiddleware) {
        this.depotRepository = depotRepository;
        this.vehicleRepository = vehicleRepository;
        this.knapsackOptimizer = knapsackOptimizer;
        this.loggingMiddleware = loggingMiddleware;
    }

    public List<Depot> getDepots() {
        List<Depot> depots = depotRepository.fetchDepots();
        loggingMiddleware.Log("backend", "info", "service", "Depots fetched successfully, count: " + depots.size());
        return depots;
    }

    public List<VehicleTask> getVehicles() {
        List<VehicleTask> vehicles = vehicleRepository.fetchVehicles();
        loggingMiddleware.Log("backend", "info", "service", "Vehicles fetched successfully, count: " + vehicles.size());
        return vehicles;
    }

    public OptimizeResponse optimizeForDepot(long depotId) {
        List<Depot> depots = getDepots();
        Depot targetDepot = depots.stream()
                .filter(depot -> depot.depotId == depotId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Depot not found: " + depotId));

        List<VehicleTask> vehicles = getVehicles();
        loggingMiddleware.Log("backend", "info", "service", "Starting knapsack optimization for depotId=" + depotId + ", budget=" + targetDepot.mechanicHours + ", taskCount=" + vehicles.size());
        OptimizationResult optimizationResult = knapsackOptimizer.optimize(vehicles, targetDepot.mechanicHours);
        loggingMiddleware.Log("backend", "info", "service", "Optimization complete for depotId=" + depotId + ", totalImpact=" + optimizationResult.totalImpactScore + ", tasksSelected=" + optimizationResult.selectedTasks.size());

        OptimizeResponse response = new OptimizeResponse();
        response.depotId = depotId;
        response.mechanicHoursBudget = targetDepot.mechanicHours;
        response.totalImpactScore = optimizationResult.totalImpactScore;
        response.totalDuration = optimizationResult.totalDuration;
        response.selectedTasks = optimizationResult.selectedTasks;
        return response;
    }

    public OptimizeAllResponse optimizeForAllDepots() {
        List<Depot> depots = getDepots();
        List<VehicleTask> vehicles = getVehicles();
        OptimizeAllResponse batchResponse = new OptimizeAllResponse();
        batchResponse.results = new ArrayList<>();

        for (Depot depot : depots) {
            loggingMiddleware.Log("backend", "info", "service", "Starting knapsack optimization for depotId=" + depot.depotId + ", budget=" + depot.mechanicHours + ", taskCount=" + vehicles.size());
            OptimizationResult optimizationResult = knapsackOptimizer.optimize(vehicles, depot.mechanicHours);
            loggingMiddleware.Log("backend", "info", "service", "Optimization complete for depotId=" + depot.depotId + ", totalImpact=" + optimizationResult.totalImpactScore + ", tasksSelected=" + optimizationResult.selectedTasks.size());

            OptimizeResponse response = new OptimizeResponse();
            response.depotId = depot.depotId;
            response.mechanicHoursBudget = depot.mechanicHours;
            response.totalImpactScore = optimizationResult.totalImpactScore;
            response.totalDuration = optimizationResult.totalDuration;
            response.selectedTasks = optimizationResult.selectedTasks;
            batchResponse.results.add(response);
        }

        return batchResponse;
    }
}
