package io.github.software.coursework;

import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.algo.PredictModel;
import io.github.software.coursework.util.TestStorage;
import org.apache.commons.lang3.time.TimeZones;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Threads(1)
public class PredictionModelBenchmark {

    private ImmutableLongArray times;
    private PredictModel predictModel;
    private long timestamp;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        predictModel = new PredictModel(new TestStorage());
        predictModel.loadTransactionsAndTrain();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZones.GMT);
        calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0); // 月份从 0 开始
        calendar.set(Calendar.MILLISECOND, 0);

        timestamp = calendar.getTimeInMillis();
        long[] arr = new long[50];

        for (int i = 0; i < 50; i++) {
            arr[i] = timestamp + 24 * 60 * 60 * 1000L * i;
        }
        times = ImmutableLongArray.copyOf(arr);
    }

    @Benchmark
    public void test(Blackhole bh) throws IOException {
        bh.consume(predictModel.predictBudgetUsageAsync(timestamp, times));
    }
}
