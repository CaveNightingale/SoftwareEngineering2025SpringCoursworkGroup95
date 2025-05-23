package io.github.software.coursework;

import io.github.software.coursework.util.XorShift128;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Threads(1)
public class XorShift128UniformBenchmark {
    private final XorShift128 xorShift = new XorShift128();

    @Benchmark
    public void test(Blackhole bh) {
        bh.consume(xorShift.nextDouble());
    }
}
