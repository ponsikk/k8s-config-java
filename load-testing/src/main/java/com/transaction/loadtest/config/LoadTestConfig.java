package com.transaction.loadtest.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoadTestConfig {
    private String targetUrl;
    private int targetRps;
    private int durationSeconds;
    private int warmupSeconds;
    private boolean progressive;
    private String progressiveStages;
}
