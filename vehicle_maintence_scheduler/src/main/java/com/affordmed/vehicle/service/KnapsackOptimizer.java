package com.affordmed.vehicle.service;

import com.affordmed.vehicle.domain.OptimizationResult;
import com.affordmed.vehicle.domain.SelectedTask;
import com.affordmed.vehicle.domain.VehicleTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class KnapsackOptimizer {

    public OptimizationResult optimize(List<VehicleTask> tasks, int capacity) {
        if (tasks == null || tasks.isEmpty() || capacity <= 0) {
            OptimizationResult emptyResult = new OptimizationResult();
            return emptyResult;
        }

        int taskCount = tasks.size();
        int[] bestImpactByCapacity = new int[capacity + 1];
        int[] lastTaskIndexByCapacity = new int[capacity + 1];
        int[] previousCapacityByCapacity = new int[capacity + 1];
        for (int index = 0; index <= capacity; index++) {
            lastTaskIndexByCapacity[index] = -1;
            previousCapacityByCapacity[index] = -1;
        }

        for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
            VehicleTask task = tasks.get(taskIndex);
            if (task.duration <= 0 || task.duration > capacity) {
                continue;
            }

            for (int currentCapacity = capacity; currentCapacity >= task.duration; currentCapacity--) {
                int candidateImpact = bestImpactByCapacity[currentCapacity - task.duration] + task.impact;
                if (candidateImpact > bestImpactByCapacity[currentCapacity]) {
                    bestImpactByCapacity[currentCapacity] = candidateImpact;
                    lastTaskIndexByCapacity[currentCapacity] = taskIndex;
                    previousCapacityByCapacity[currentCapacity] = currentCapacity - task.duration;
                }
            }
        }

        List<SelectedTask> selectedTasks = new ArrayList<>();
        int currentCapacity = capacity;
        while (currentCapacity >= 0 && lastTaskIndexByCapacity[currentCapacity] != -1) {
            VehicleTask task = tasks.get(lastTaskIndexByCapacity[currentCapacity]);
            SelectedTask selectedTask = new SelectedTask();
            selectedTask.taskId = task.taskId;
            selectedTask.duration = task.duration;
            selectedTask.impact = task.impact;
            selectedTasks.add(selectedTask);
            currentCapacity = previousCapacityByCapacity[currentCapacity];
        }

        Collections.reverse(selectedTasks);

        OptimizationResult result = new OptimizationResult();
        result.selectedTasks = selectedTasks;
        for (SelectedTask selectedTask : selectedTasks) {
            result.totalImpactScore += selectedTask.impact;
            result.totalDuration += selectedTask.duration;
        }
        return result;
    }
}
