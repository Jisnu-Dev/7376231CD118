package com.affordmed.vehicle.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VehicleTask {

    @JsonProperty("TaskID")
    public String taskId;

    @JsonProperty("Duration")
    public int duration;

    @JsonProperty("Impact")
    public int impact;
}
