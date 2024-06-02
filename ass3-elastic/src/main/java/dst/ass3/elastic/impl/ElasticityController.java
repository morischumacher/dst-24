package dst.ass3.elastic.impl;

import dst.ass3.elastic.ContainerException;
import dst.ass3.elastic.ContainerInfo;
import dst.ass3.elastic.IContainerService;
import dst.ass3.elastic.IElasticityController;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElasticityController implements IElasticityController {

    // Container service for managing containers
    private final IContainerService containerService;
    // Workload monitor for monitoring workload
    private final IWorkloadMonitor monitor;

    // Constructor
    public ElasticityController(IContainerService containerService, IWorkloadMonitor monitor) {
        this.containerService = containerService;
        this.monitor = monitor;
    }

    // Inner class for region information
    static private class RegionInfo {
        public Long workerCount;
        public Long requestCount;
        public Double avgTime;
        public Long maxWaittime;

        // Constructor
        public RegionInfo(Long workerCount, Long requestCount, Double avgTime, Long maxWaittime) {
            this.workerCount = workerCount;
            this.requestCount = requestCount;
            this.avgTime = avgTime;
            this.maxWaittime = maxWaittime;
        }
    }

    // Method to calculate optimal worker count
    private Long calculateOptimalWorkercount(RegionInfo info) {
        // Define the scale out and scale down thresholds
        double scaleOutThreshold = 0.1;
        double scaleDownThreshold = 0.05;

        // Initialize the optimal worker count to the current worker count
        Long optimalWorkers = info.workerCount;

        // Calculate the linear waiting time
        double linearWaitingTime = info.requestCount * info.avgTime;

        // Calculate the expected waiting time
        double exp = linearWaitingTime / optimalWorkers;

        // If the expected waiting time is greater than the maximum waiting time plus the scale out threshold,
        // increment the optimal worker count until the expected waiting time is less than or equal to the maximum waiting time
        if (exp > info.maxWaittime * (1 + scaleOutThreshold)) {
            while ((linearWaitingTime / optimalWorkers) > info.maxWaittime) {
                optimalWorkers++;
            }
        }
        // If the expected waiting time is less than the maximum waiting time minus the scale down threshold,
        // decrement the optimal worker count until the expected waiting time is greater than or equal to the maximum waiting time
        else if (exp < info.maxWaittime * (1 - scaleDownThreshold)) {
            while ((linearWaitingTime / optimalWorkers) < info.maxWaittime && optimalWorkers > 0) {
                optimalWorkers--;
            }
        }

        // Return the calculated optimal worker count
        return optimalWorkers;
    }

    // Method to adjust workers
    @Override
    public void adjustWorkers() throws ContainerException {
        // Get the worker counts, request counts, and average processing times from the monitor
        Map<Region, Long> workerCounts = monitor.getWorkerCount();
        Map<Region, Long> requestCounts = monitor.getRequestCount();
        Map<Region, Double> avgTimes = monitor.getAverageProcessingTime();

        // Define the maximum wait times for each region
        Map<Region, Long> maxWaitTimesMS = Map.of(
                Region.AT_VIENNA, 30L * 1000,
                Region.AT_LINZ, 30L * 1000,
                Region.DE_BERLIN, 120L * 1000
        );

        // Iterate over each region
        for (Region r : Region.values()) {
            // Create a new RegionInfo object for the current region
            RegionInfo info = new RegionInfo(
                    workerCounts.getOrDefault(r, 0L),
                    requestCounts.getOrDefault(r, 0L),
                    avgTimes.getOrDefault(r, 0.0),
                    maxWaitTimesMS.get(r)
            );

            // Calculate the optimal worker count for the current region
            Long optimalWorkerCount = calculateOptimalWorkercount(info);

            // Calculate the difference between the optimal worker count and the current worker count
            Long delta = optimalWorkerCount - info.workerCount;

            // If the delta is positive, spawn new workers
            if (delta > 0) {
                spawnWorkers(r, delta);
            }
            // If the delta is negative, remove workers
            else if (delta < 0) {
                removeWorkers(r, -delta);
            }
        }
    }

    private void spawnWorkers(Region region, Long delta) throws ContainerException {
        for (int i = 0; i < delta; i++) {
            containerService.startWorker(region);
        }
    }

    private void removeWorkers(Region region, Long delta) throws ContainerException {
        List<ContainerInfo> regionContainers = containerService
                .listContainers()
                .stream()
                .filter(c -> c.getWorkerRegion() == region)
                .collect(Collectors.toList());

        for (int i = 0; i < delta; i++) {
            containerService.stopContainer(regionContainers.get(i).getContainerId());
        }
    }
}