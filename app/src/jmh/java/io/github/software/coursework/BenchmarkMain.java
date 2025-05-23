package io.github.software.coursework;

import com.google.common.collect.ImmutableMap;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;
import java.util.Map;

public class BenchmarkMain {
    // Expected to be 3 times faster in desktop CPUs than the minimum requirement
    private static final Map<String, Double> minimumThroughputRequirement = ImmutableMap.of(
            XorShift128UniformBenchmark.class.getName(), 1.5e8,
            XorShift128GaussianBenchmark.class.getName(), 5.0e7,
            PredictionModelBenchmark.class.getName(), 1e0
    );

    public static void main(String[] args) throws RunnerException {
        ChainedOptionsBuilder opt = new OptionsBuilder()
                .forks(1)
                .warmupIterations(10)
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(10));
        for (String className : minimumThroughputRequirement.keySet()) {
            opt.include(className.replaceAll("\\.", "\\\\.") + ".*");
        }

        Collection<RunResult> results = new Runner(opt.build()).run();

        boolean allPassed = true;
        for (RunResult rr : results) {
            String benchmarkName = rr.getParams().getBenchmark();
            String className = benchmarkName.substring(0, benchmarkName.lastIndexOf('.'));
            double score = rr.getPrimaryResult().getScore();
            double minRequirement = minimumThroughputRequirement.getOrDefault(className, 0.0);

            System.out.printf("%s: %.2f ops/s (min requirement: %.2f ops/s)%n", benchmarkName, score, minRequirement);
            if (score < minRequirement) {
                System.err.printf("Benchmark %s did not meet the minimum requirement!%n", benchmarkName);
                allPassed = false;
            }
        }
        if (!allPassed) {
            throw new AssertionError("Some time-critical functions are extremely slow! Please check the implementation.");
        }
    }
}
