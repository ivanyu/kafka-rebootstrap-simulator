package org.apache.kafka.clients;  // to access ClusterConnectionStates easily

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebootstrapSimulator {
    private static final Logger LOG = LoggerFactory.getLogger(RebootstrapSimulator.class);
    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws ParseException, ExecutionException, InterruptedException {
        Options options = new Options();

        Option threadsOpt = Option.builder()
            .longOpt("threads")
            .argName("threads")
            .hasArg()
            .type(Integer.class)
            .build();
        options.addOption(threadsOpt);

        Option runsOpt = Option.builder()
            .longOpt("runs")
            .argName("runs")
            .hasArg()
            .type(Integer.class)
            .required()
            .build();
        options.addOption(runsOpt);

        Option durationOpt = Option.builder()
            .longOpt("duration")
            .argName("duration")
            .hasArg()
            .type(Duration.class)
            .converter(RebootstrapSimulator::parseDuration)
            .required()
            .build();
        options.addOption(durationOpt);

        Option tickOpt = Option.builder()
            .longOpt("tick")
            .argName("tick")
            .hasArg()
            .type(Long.class)
            .required()
            .build();
        options.addOption(tickOpt);

        Option nodesOpt = Option.builder()
            .longOpt("nodes")
            .argName("nodes")
            .hasArg()
            .type(Integer.class)
            .required()
            .build();
        options.addOption(nodesOpt);

        Option reconnectBackoffMsOpt = Option.builder()
            .longOpt("reconnectBackoffMs")
            .argName("reconnectBackoffMs")
            .hasArg()
            .type(Long.class)
            .required()
            .build();
        options.addOption(reconnectBackoffMsOpt);

        Option reconnectBackoffMaxMsOpt = Option.builder()
            .longOpt("reconnectBackoffMaxMs")
            .argName("reconnectBackoffMaxMs")
            .hasArg()
            .type(Long.class)
            .required()
            .build();
        options.addOption(reconnectBackoffMaxMsOpt);

        Option connectionSetupTimeoutMsOpt = Option.builder()
            .longOpt("connectionSetupTimeoutMs")
            .argName("connectionSetupTimeoutMs")
            .hasArg()
            .type(Long.class)
            .required()
            .build();
        options.addOption(connectionSetupTimeoutMsOpt);

        Option connectionSetupTimeoutMaxMsOpt = Option.builder()
            .longOpt("connectionSetupTimeoutMaxMs")
            .argName("connectionSetupTimeoutMaxMs")
            .hasArg()
            .type(Long.class)
            .required()
            .build();
        options.addOption(connectionSetupTimeoutMaxMsOpt);

        CommandLine cmd = new DefaultParser().parse(options, args);

        Integer threads = cmd.<Integer>getParsedOptionValue(threadsOpt);
        if (threads == null) {
            threads = Runtime.getRuntime().availableProcessors();
        }

        int runs = cmd.<Integer>getParsedOptionValue(runsOpt);

        Duration duration = cmd.getParsedOptionValue(durationOpt);
        long tick = cmd.<Long>getParsedOptionValue(tickOpt);
        int nodes = cmd.<Integer>getParsedOptionValue(nodesOpt);
        long reconnectBackoffMs = cmd.<Long>getParsedOptionValue(reconnectBackoffMsOpt);
        long reconnectBackoffMaxMs = cmd.<Long>getParsedOptionValue(reconnectBackoffMaxMsOpt);
        long connectionSetupTimeoutMs = cmd.<Long>getParsedOptionValue(connectionSetupTimeoutMsOpt);
        long connectionSetupTimeoutMaxMs = cmd.<Long>getParsedOptionValue(connectionSetupTimeoutMaxMsOpt);

        SimulationParams simulationParams = new SimulationParams(
            duration, tick, nodes,
            reconnectBackoffMs, reconnectBackoffMaxMs,
            connectionSetupTimeoutMs, connectionSetupTimeoutMaxMs
        );

        LOG.info("Doing {} runs using {} threads. Params: {}", runs, threads, simulationParams);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<BatchResult>> batchFutures = new ArrayList<>();
        for (int i = 0; i < runs; i += BATCH_SIZE) {
            int runNFrom = i;
            int runNToExcl = Math.min(i + BATCH_SIZE, runs);
            Future<BatchResult> future = executor.submit(
                () -> runBatch(runNFrom, runNToExcl, simulationParams)
            );
            batchFutures.add(future);
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        int actualRuns = 0;
        for (Future<BatchResult> future : batchFutures) {
            BatchResult result = future.get();
            actualRuns += result.times().size();
            result.times().forEach(stats::addValue);
        }
        executor.shutdown();

        // Sanity check.
        if (actualRuns != runs) {
            throw new RuntimeException("Actual runs " + actualRuns + " is different from requested " + runs);
        }

        System.out.printf("Mean: %ds SD: %ds p90: %ds p95: %ds p99: %ds p99.9: %ds\n",
            (int) (stats.getMean() / 1000),
            (int) (stats.getStandardDeviation() / 1000),
            (int) (stats.getPercentile(90) / 1000),
            (int) (stats.getPercentile(95) / 1000),
            (int) (stats.getPercentile(99) / 1000),
            (int) (stats.getPercentile(99.9) / 1000)
        );
    }

    private static Duration parseDuration(String value) {
        Pattern pattern = Pattern.compile("^(\\d+)(m|min)$");
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            throw new RuntimeException("Cannot parse " + value + " as duration. Use e.g. '5m' or '5min'");
        }
        long minutes = Long.parseLong(matcher.group(1));
        return Duration.ofMinutes(minutes);
    }

    private static BatchResult runBatch(int runNFromIncl, int runNToExcl, SimulationParams params) {
        Instant started = Instant.now();
        List<Long> results = new ArrayList<>();
        for (int runN = runNFromIncl; runN < runNToExcl; runN++) {
            Long result = new Simulation(params).run();
            if (result == null) {
                // Since we're dealing with percentiles in the end, this is better than null or -1.
                result = Long.MAX_VALUE;
            }
            results.add(result);
        }
        LOG.info("Batch [{}..{}) finished, took {} s",
            runNFromIncl, runNToExcl,
            Duration.between(started, Instant.now()).toSeconds());
        return new BatchResult(results);
    }

    private record BatchResult(List<Long> times) { }
}
