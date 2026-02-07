package com.transaction.loadtest.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.loadtest.config.LoadTestConfig;
import com.transaction.loadtest.generator.TransactionGenerator;
import com.transaction.loadtest.metrics.LoadTestMetrics;
import com.transaction.models.Transaction;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoadTestRunner {

    private final LoadTestConfig config;
    private final TransactionGenerator generator;
    private final ObjectMapper objectMapper;
    private final LoadTestMetrics metrics;
    private final CloseableHttpClient httpClient;

    public LoadTestRunner(LoadTestConfig config, TransactionGenerator generator, ObjectMapper objectMapper) {
        this.config = config;
        this.generator = generator;
        this.objectMapper = objectMapper;
        this.metrics = new LoadTestMetrics();

        // Configure HTTP client with connection pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(10000);
        connectionManager.setDefaultMaxPerRoute(10000);

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public void run() {
        System.out.println("Starting load test...\n");

        if (config.isProgressive() && config.getProgressiveStages() != null) {
            runProgressiveLoad();
        } else {
            runConstantLoad();
        }

        printFinalSummary();

        try {
            httpClient.close();
        } catch (Exception e) {
            System.err.println("Error closing HTTP client: " + e.getMessage());
        }
    }

    private void runProgressiveLoad() {
        List<Stage> stages = parseProgressiveStages(config.getProgressiveStages());

        System.out.println("Progressive Load Test - " + stages.size() + " stages");
        System.out.println("-".repeat(80));

        for (int i = 0; i < stages.size(); i++) {
            Stage stage = stages.get(i);
            System.out.println("\nStage " + (i + 1) + "/" + stages.size() +
                             ": " + stage.rps + " RPS for " + stage.durationSeconds + " seconds");
            System.out.println("-".repeat(80));

            runLoadTest(stage.rps, stage.durationSeconds, 0);

            if (i < stages.size() - 1) {
                System.out.println("\nCompleted stage " + (i + 1) + ". Moving to next stage...\n");
            }
        }
    }

    private void runConstantLoad() {
        if (config.getWarmupSeconds() > 0) {
            System.out.println("Warmup Phase: " + config.getWarmupSeconds() + " seconds at " +
                             config.getTargetRps() + " RPS");
            System.out.println("-".repeat(80));
            runLoadTest(config.getTargetRps(), config.getWarmupSeconds(), 0);
            metrics.reset();
            System.out.println("\nWarmup complete. Starting actual test...\n");
        }

        System.out.println("Main Test: " + config.getDurationSeconds() + " seconds at " +
                         config.getTargetRps() + " RPS");
        System.out.println("-".repeat(80));
        runLoadTest(config.getTargetRps(), config.getDurationSeconds(), 5);
    }

    private void runLoadTest(int targetRps, int durationSeconds, int statsIntervalSeconds) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        ScheduledExecutorService statsExecutor = null;

        if (statsIntervalSeconds > 0) {
            statsExecutor = Executors.newScheduledThreadPool(1);
            statsExecutor.scheduleAtFixedRate(
                this::printRealtimeStats,
                statsIntervalSeconds,
                statsIntervalSeconds,
                TimeUnit.SECONDS
            );
        }

        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(durationSeconds);

        // Calculate delay between requests in nanoseconds
        long delayNanos = 1_000_000_000L / targetRps;

        AtomicBoolean running = new AtomicBoolean(true);
        List<Future<?>> futures = new ArrayList<>();

        // Start request generator
        Future<?> generatorFuture = executor.submit(() -> {
            long nextRequestTime = System.nanoTime();

            while (running.get() && Instant.now().isBefore(endTime)) {
                long currentTime = System.nanoTime();

                if (currentTime >= nextRequestTime) {
                    executor.submit(this::sendRequest);
                    nextRequestTime += delayNanos;
                } else {
                    // Sleep for a short time to avoid busy-waiting
                    long sleepNanos = Math.min(delayNanos / 10, nextRequestTime - currentTime);
                    if (sleepNanos > 0) {
                        try {
                            Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            running.set(false);
        });

        futures.add(generatorFuture);

        // Wait for test duration
        try {
            generatorFuture.get();
        } catch (Exception e) {
            System.err.println("Error during load test: " + e.getMessage());
        }

        // Shutdown executors
        executor.shutdown();
        if (statsExecutor != null) {
            statsExecutor.shutdown();
        }

        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
            if (statsExecutor != null) {
                statsExecutor.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print final stats for this phase
        if (statsIntervalSeconds > 0) {
            System.out.println();
            printRealtimeStats();
        }
    }

    private void sendRequest() {
        Transaction transaction = generator.generate();

        long startTime = System.nanoTime();

        try {
            String json = objectMapper.writeValueAsString(transaction);

            HttpPost post = new HttpPost(config.getTargetUrl());
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getCode();
                long latencyMs = (System.nanoTime() - startTime) / 1_000_000;

                if (statusCode >= 200 && statusCode < 300) {
                    metrics.recordSuccess(latencyMs);
                } else {
                    metrics.recordFailure();
                }
            }
        } catch (Exception e) {
            long latencyMs = (System.nanoTime() - startTime) / 1_000_000;
            metrics.recordFailure();
        }
    }

    private void printRealtimeStats() {
        LoadTestMetrics.MetricsStats stats = metrics.getStats();

        System.out.printf("[Stats] Total: %d | Success: %d (%.2f%%) | Failed: %d | " +
                        "Latency (ms) - Min: %d | Avg: %.2f | p50: %d | p95: %d | p99: %d | Max: %d%n",
                stats.totalRequests,
                stats.successfulRequests,
                stats.getSuccessRate(),
                stats.failedRequests,
                stats.minLatency,
                stats.avgLatency,
                stats.p50Latency,
                stats.p95Latency,
                stats.p99Latency,
                stats.maxLatency
        );
    }

    private void printFinalSummary() {
        LoadTestMetrics.MetricsStats stats = metrics.getStats();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("  FINAL SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Total Requests:      " + stats.totalRequests);
        System.out.println("Successful:          " + stats.successfulRequests +
                         String.format(" (%.2f%%)", stats.getSuccessRate()));
        System.out.println("Failed:              " + stats.failedRequests);
        System.out.println();
        System.out.println("Latency (ms):");
        System.out.println("  Min:               " + stats.minLatency);
        System.out.println("  Average:           " + String.format("%.2f", stats.avgLatency));
        System.out.println("  p50 (Median):      " + stats.p50Latency);
        System.out.println("  p95:               " + stats.p95Latency);
        System.out.println("  p99:               " + stats.p99Latency);
        System.out.println("  Max:               " + stats.maxLatency);
        System.out.println();
        System.out.println("=".repeat(80));
    }

    private List<Stage> parseProgressiveStages(String stagesStr) {
        List<Stage> stages = new ArrayList<>();
        String[] stageTokens = stagesStr.split(",");

        for (String stageToken : stageTokens) {
            String[] parts = stageToken.trim().split(":");
            if (parts.length == 2) {
                int rps = Integer.parseInt(parts[0].trim());
                int duration = Integer.parseInt(parts[1].trim());
                stages.add(new Stage(rps, duration));
            }
        }

        return stages;
    }

    private static class Stage {
        final int rps;
        final int durationSeconds;

        Stage(int rps, int durationSeconds) {
            this.rps = rps;
            this.durationSeconds = durationSeconds;
        }
    }
}
