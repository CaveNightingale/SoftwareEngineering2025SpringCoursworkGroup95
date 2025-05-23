package io.github.software.coursework;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.algo.PredictModel;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.NoSuchDocumentException;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Goal;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.XorShift128;
import org.apache.commons.lang3.time.TimeZones;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

final class TestStorage implements AsyncStorage {

    static class TestEntityTable implements EntityTable {

        private final ReferenceItemPair<Entity>[] entities;

        public TestEntityTable() {
            String nameList =
                    """
                    北京邮电大学学生第一食堂
                    北京邮电大学学生第二食堂
                    北邮风味餐厅
                    北邮清真食堂
                    北邮教工食堂
                    北邮食堂二楼自助餐区
                    北邮校园咖啡吧
                    每日优鲜
                    北京邮电大学小卖部
                    盒马鲜生
                    小麦铺
                    北邮食堂烘焙窗口
                    北邮鲜榨果汁吧
                    北邮早餐供应点
                    北邮夜宵窗口
                    美团外卖
                    饿了么
                    肯德基宅急送
                    麦当劳麦乐送
                    海底捞火锅
                    西贝莜面村
                    麦当劳
                    必胜客
                    星巴克
                    瑞幸咖啡
                    喜茶
                    奈雪的茶
                    一点点
                    蜜雪冰城
                    达美乐比萨
                    便利蜂
                    711便利店
                    嘉禾一品
                    和合谷
                    张亮麻辣烫
                    杨国福麻辣烫
                    庆丰包子铺
                    眉州东坡酒楼
                    云海肴云南菜
                    鲜芋仙
                    北邮自动贩卖机
                    校园水果店
                    美团买菜
                    泸溪河桃酥
                    """;
            String[] names = nameList.split("\n");
            entities = (ReferenceItemPair<Entity>[]) new ReferenceItemPair[names.length];
            for (int i = 0; i < names.length; i++) {
                Entity x = new Entity(names[i], "", "", "", "", Entity.Type.COMMERCIAL);
                entities[i] = new ReferenceItemPair<>(new Reference<>(i), x);
            }
        }

        @Override
        public SequencedCollection<ReferenceItemPair<Entity>> list(int offset, int limit) throws IOException {
            return Arrays.asList(entities).subList(offset, Math.min(offset + limit, entities.length));
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public @Nullable Entity put(Reference<Entity> key, Sensitivity sensitivity, @Nullable Entity value) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Entity get(Reference<Entity> key) throws IOException {
            int index = Arrays.binarySearch(entities, new ReferenceItemPair<>(key, null), Comparator.comparingLong(a -> a.reference().id()));
            if (index == -1) {
                throw new NoSuchDocumentException("");
            }
            return entities[index].item();
        }
    }

    static class TestTransactionTable implements TransactionTable {

        private final ReferenceItemPair<Transaction>[] transactions;

        @SuppressWarnings("unchecked")
        public TestTransactionTable() {
            Calendar calendar = Calendar.getInstance();

            calendar.setTimeZone(TimeZones.GMT);

            calendar.set(2023, Calendar.OCTOBER, 1, 0, 0, 0); // 月份从 0 开始

            calendar.set(Calendar.MILLISECOND, 0);

            long timestamp = calendar.getTimeInMillis();

            System.out.println(timestamp);

            transactions = (ReferenceItemPair<Transaction>[]) new ReferenceItemPair[1000];

            int i = 0;
            for (long time = timestamp - 24 * 60 * 60 * 1000L * 1000; time < timestamp; time += 24 * 60 * 60 * 1000L) {
                Transaction x = new Transaction("", "", time, (int)(Math.random() * 400000) - 200000, "Diet" + i, new Reference<>((int) (Math.random() * 20)), ImmutableList.of());
                transactions[i] = new ReferenceItemPair<>(new Reference<>(i), x );
                i++;
            }
        }

        @Override
        public SequencedCollection<ReferenceItemPair<Transaction>> list(long start, long end, int offset, int limit) throws IOException {
            return Arrays.stream(transactions)
                    .filter(x -> x.item().time() > start && x.item().time() <= end)
                    .skip(offset)
                    .limit(limit)
                    .toList()
                    .reversed();
        }

        @Override
        public ImmutablePair<Set<String>, Set<String>> getCategories() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCategory(String category, Sensitivity sensitivity) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeCategory(String category, Sensitivity sensitivity) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ImmutablePair<Set<String>, Set<String>> getTags() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addTag(String tag, Sensitivity sensitivity) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeTag(String tag, Sensitivity sensitivity) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Goal getGoal() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setGoal(@Nullable Goal goal, Sensitivity sensitivity) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public @Nullable Transaction put(Reference<Transaction> key, Sensitivity sensitivity, @Nullable Transaction value) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction get(Reference<Transaction> key) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private final TestEntityTable testEntityTable = new TestEntityTable();
    private final TestTransactionTable testTransactionTable = new TestTransactionTable();

    @Override
    public void entity(Consumer<EntityTable> callback) {
        callback.accept(testEntityTable);
    }

    @Override
    public void transaction(Consumer<TransactionTable> callback) {
        callback.accept(testTransactionTable);
    }

    @Override
    public void model(Consumer<ModelDirectory> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> close() {
        throw new UnsupportedOperationException();
    }
}

public class PredictionModelTest {

    @Test
    public void testRandom() {
        XorShift128 rand = new XorShift128();
        for (int i=0; i<10; i++) {
            System.out.println(rand.nextGaussian());
        }
    }

//    @Test
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

        CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> future =
                predictModel.predictBudgetUsage(millis, time);
        try {
            ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>> result = future.get(); // 或 future.join()

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
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        future = predictModel.predictSavedAmount(millis, time);
        try {
            ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>> result = future.get(); // 或 future.join()

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
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
