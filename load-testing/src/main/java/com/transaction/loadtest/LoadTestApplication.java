package com.transaction.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transaction.loadtest.config.LoadTestConfig;
import com.transaction.loadtest.generator.TransactionGenerator;
import com.transaction.loadtest.runner.LoadTestRunner;
import org.apache.commons.cli.*;

public class LoadTestApplication {

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("u", "url", true, "Target URL (default: http://localhost:8080/api/v1/transactions)");
        options.addOption("r", "rps", true, "Target requests per second (default: 1000)");
        options.addOption("d", "duration", true, "Test duration in seconds (default: 60)");
        options.addOption("w", "warmup", true, "Warmup duration in seconds (default: 10)");
        options.addOption("p", "progressive", false, "Enable progressive load (ramp-up)");
        options.addOption("s", "stages", true, "Progressive stages: rps1:duration1,rps2:duration2,... (e.g., 1000:30,5000:60,10000:30)");
        options.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("load-tester", options);
                return;
            }

            LoadTestConfig config = LoadTestConfig.builder()
                    .targetUrl(cmd.getOptionValue("url", "http://localhost:8080/api/v1/transactions"))
                    .targetRps(Integer.parseInt(cmd.getOptionValue("rps", "1000")))
                    .durationSeconds(Integer.parseInt(cmd.getOptionValue("duration", "60")))
                    .warmupSeconds(Integer.parseInt(cmd.getOptionValue("warmup", "10")))
                    .progressive(cmd.hasOption("progressive"))
                    .progressiveStages(cmd.getOptionValue("stages"))
                    .build();

            System.out.println("=".repeat(80));
            System.out.println("  TRANSACTION SYSTEM LOAD TESTER (Java 21 Virtual Threads)");
            System.out.println("=".repeat(80));
            System.out.println("Configuration:");
            System.out.println("  Target URL:       " + config.getTargetUrl());
            System.out.println("  Target RPS:       " + config.getTargetRps());
            System.out.println("  Duration:         " + config.getDurationSeconds() + "s");
            System.out.println("  Warmup:           " + config.getWarmupSeconds() + "s");
            System.out.println("  Progressive Load: " + (config.isProgressive() ? "YES" : "NO"));
            if (config.isProgressive() && config.getProgressiveStages() != null) {
                System.out.println("  Stages:           " + config.getProgressiveStages());
            }
            System.out.println("=".repeat(80));
            System.out.println();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            TransactionGenerator generator = new TransactionGenerator();
            LoadTestRunner runner = new LoadTestRunner(config, generator, objectMapper);

            runner.run();

        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            formatter.printHelp("load-tester", options);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error running load test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
