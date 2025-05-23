package io.github.software.coursework;

import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.algo.PredictModel;
import io.github.software.coursework.util.TestStorage;
import io.github.software.coursework.util.XorShift128;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

public class PredictionModelTest {

    @Test
    public void testRandom() {
        XorShift128 rand = new XorShift128();
        for (int i=0; i<10; i++) {
            System.out.println(rand.nextGaussian());
        }
    }

    @Test
    public void testPredictionModel() {
        ZoneId zone = ZoneId.of("UTC");

        LocalDate today = LocalDate.now(zone);
        ZonedDateTime startOfDay = today.atStartOfDay(zone);

        long millis = startOfDay.toInstant().toEpochMilli();

        System.out.println("Start of today (ms): " + millis);

        long millisPerDay = 1000L * 60 * 60 * 24;

        assert(millis % millisPerDay == 0);

        long[] t = new long[60];
        for (int i = 0; i < 60; i++)
            t[i] = millis + millisPerDay * i;

        ImmutableLongArray time = ImmutableLongArray.copyOf(t);

        TestStorage testStorage = new TestStorage();

        PredictModel predictModel = new PredictModel(testStorage);
        predictModel.loadTransactionsAndTrain();

//        CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> future =
//                predictModel.predictBudgetUsage(millis, time);
//            ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>> result = future.get(); // 或 future.join()
        ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>> result = predictModel.predictBudgetUsageAsync(millis, time);
        // 提取数据
        ImmutableDoubleArray part1 = result.left;
        ImmutableDoubleArray part2 = result.right.getLeft();
        ImmutableDoubleArray part3 = result.right.getRight();

//            System.out.println("Part 1: " + part1);
//            System.out.println("Part 2: " + part2);
//            System.out.println("Part 3: " + part3);

        for (int i = 0; i < part1.length(); i++) {
            double v1 = part1.get(i);
            double v2 = part2.get(i);
            double v3 = part3.get(i);

            assertFalse(Double.isNaN(v1), "part1[" + i + "] is NaN");
            assertFalse(Double.isInfinite(v1), "part1[" + i + "] is Infinite");

            assertFalse(Double.isNaN(v2), "part2[" + i + "] is NaN");
            assertFalse(Double.isInfinite(v2), "part2[" + i + "] is Infinite");

            assertFalse(Double.isNaN(v3), "part3[" + i + "] is NaN");
            assertFalse(Double.isInfinite(v3), "part3[" + i + "] is Infinite");

            assertTrue(v1 >= v2, "part1[" + i + "] < part2[" + i + "]");
            assertTrue(v1 <= v3, "part1[" + i + "] > part3[" + i + "]");
        }

//        future = predictModel.predictSavedAmount(millis, time);
//            ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>> result = future.get(); // 或 future.join()

        result = predictModel.predictSavedAmountAsync(millis, time);
        // 提取数据
        part1 = result.left;
        part2 = result.right.getLeft();
        part3 = result.right.getRight();

        for (int i = 0; i < part1.length(); i++) {
            double v1 = part1.get(i);
            double v2 = part2.get(i);
            double v3 = part3.get(i);

            assertFalse(Double.isNaN(v1), "part1[" + i + "] is NaN");
            assertFalse(Double.isInfinite(v1), "part1[" + i + "] is Infinite");

            assertFalse(Double.isNaN(v2), "part2[" + i + "] is NaN");
            assertFalse(Double.isInfinite(v2), "part2[" + i + "] is Infinite");

            assertFalse(Double.isNaN(v3), "part3[" + i + "] is NaN");
            assertFalse(Double.isInfinite(v3), "part3[" + i + "] is Infinite");

            assertTrue(v1 >= v2, "part1[" + i + "] < part2[" + i + "]");
            assertTrue(v1 <= v3, "part1[" + i + "] > part3[" + i + "]");
        }
    }
}
