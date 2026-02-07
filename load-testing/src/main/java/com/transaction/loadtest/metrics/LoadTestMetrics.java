package com.transaction.loadtest.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestMetrics {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final ConcurrentSkipListMap<Long, Long> latencies = new ConcurrentSkipListMap<>();
    private final AtomicLong latencySequence = new AtomicLong(0);

    public void recordSuccess(long latencyMs) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        // Use sequence counter as key to maintain order and allow duplicates
        latencies.put(latencySequence.getAndIncrement(), latencyMs);
    }

    public void recordFailure() {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
    }

    public MetricsStats getStats() {
        long total = totalRequests.get();
        long successful = successfulRequests.get();
        long failed = failedRequests.get();

        if (latencies.isEmpty()) {
            return new MetricsStats(total, successful, failed, 0, 0, 0, 0, 0, 0);
        }

        List<Long> latencyList = new ArrayList<>(latencies.values());
        Collections.sort(latencyList);

        long min = latencyList.get(0);
        long max = latencyList.get(latencyList.size() - 1);
        long sum = latencyList.stream().mapToLong(Long::longValue).sum();
        double avg = (double) sum / latencyList.size();

        long p50 = getPercentile(latencyList, 50);
        long p95 = getPercentile(latencyList, 95);
        long p99 = getPercentile(latencyList, 99);

        return new MetricsStats(total, successful, failed, min, max, avg, p50, p95, p99);
    }

    private long getPercentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        latencies.clear();
        latencySequence.set(0);
    }

    public static class MetricsStats {
        public final long totalRequests;
        public final long successfulRequests;
        public final long failedRequests;
        public final long minLatency;
        public final long maxLatency;
        public final double avgLatency;
        public final long p50Latency;
        public final long p95Latency;
        public final long p99Latency;

        public MetricsStats(long totalRequests, long successfulRequests, long failedRequests,
                           long minLatency, long maxLatency, double avgLatency,
                           long p50Latency, long p95Latency, long p99Latency) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.minLatency = minLatency;
            this.maxLatency = maxLatency;
            this.avgLatency = avgLatency;
            this.p50Latency = p50Latency;
            this.p95Latency = p95Latency;
            this.p99Latency = p99Latency;
        }

        public double getSuccessRate() {
            return totalRequests > 0 ? (successfulRequests * 100.0 / totalRequests) : 0;
        }
    }
}
