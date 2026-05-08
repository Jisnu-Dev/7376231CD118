package com.affordmed.vehicle.controller;

import com.affordmed.middleware.LoggingMiddleware;
import com.affordmed.vehicle.domain.Depot;
import com.affordmed.vehicle.domain.OptimizeAllResponse;
import com.affordmed.vehicle.domain.OptimizeRequest;
import com.affordmed.vehicle.domain.OptimizeResponse;
import com.affordmed.vehicle.domain.VehicleTask;
import com.affordmed.vehicle.service.VehicleSchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-scheduling")
public class VehicleSchedulingController {

    private final VehicleSchedulingService vehicleSchedulingService;
    private final LoggingMiddleware loggingMiddleware;

    public VehicleSchedulingController(VehicleSchedulingService vehicleSchedulingService, LoggingMiddleware loggingMiddleware) {
        this.vehicleSchedulingService = vehicleSchedulingService;
        this.loggingMiddleware = loggingMiddleware;
    }

    @GetMapping("/depots")
    public ResponseEntity<List<Depot>> getDepots() {
        loggingMiddleware.Log("backend", "info", "route", "Fetching depots from external API");
        return ResponseEntity.ok(vehicleSchedulingService.getDepots());
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleTask>> getVehicles() {
        loggingMiddleware.Log("backend", "info", "route", "Fetching vehicles from external API");
        return ResponseEntity.ok(vehicleSchedulingService.getVehicles());
    }

    @PostMapping("/optimize")
    public ResponseEntity<OptimizeResponse> optimize(@Valid @RequestBody OptimizeRequest request) {
        loggingMiddleware.Log("backend", "info", "route", "Optimization request received for depotId=" + request.depotId);
        return ResponseEntity.ok(vehicleSchedulingService.optimizeForDepot(request.depotId));
    }

    @GetMapping("/optimize/all")
    public ResponseEntity<OptimizeAllResponse> optimizeAll() {
        loggingMiddleware.Log("backend", "info", "route", "Optimization request received for all depots");
        return ResponseEntity.ok(vehicleSchedulingService.optimizeForAllDepots());
    }
}
