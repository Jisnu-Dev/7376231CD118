package com.affordmed.vehicle.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Depot {

    @JsonProperty("ID")
    public long depotId;

    @JsonProperty("MechanicHours")
    public int mechanicHours;
}
