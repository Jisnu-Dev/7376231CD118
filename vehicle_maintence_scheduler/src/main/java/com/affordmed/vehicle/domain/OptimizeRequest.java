package com.affordmed.vehicle.domain;

import jakarta.validation.constraints.Positive;

public class OptimizeRequest {

    @Positive(message = "depotId must be positive")
    public long depotId;
}
