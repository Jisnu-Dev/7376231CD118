package com.affordmed.vehicle.domain;

import java.util.ArrayList;
import java.util.List;

public class OptimizeResponse {

    public long depotId;
    public int mechanicHoursBudget;
    public int totalImpactScore;
    public int totalDuration;
    public List<SelectedTask> selectedTasks = new ArrayList<>();
}
