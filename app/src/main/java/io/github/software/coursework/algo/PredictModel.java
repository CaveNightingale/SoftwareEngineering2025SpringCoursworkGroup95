package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.EntityPrediction;
import io.github.software.coursework.TagPrediction;
import io.github.software.coursework.algo.probmodel.GMModelCalculation;
import io.github.software.coursework.algo.probmodel.GaussMixtureModel;
import io.github.software.coursework.data.*;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.Bitmask;
import io.github.software.coursework.util.XorShift128;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class PredictModel implements Model {

    public static final int MONT_COUNT = 1024;
    private static final int montLower = (int) (0.05 * MONT_COUNT);
    private static final int montUpper = (int) (0.95 * MONT_COUNT);

    public GaussMixtureModel gaussMixtureModel;
    public EntityPrediction entityPrediction1;
    public EntityPrediction entityPrediction2;
    public Map<String, double[][]> GMModelParameters;
    private final AsyncStorage storage;
    public int changedFlag;
    public TagPrediction tagPrediction;

    private record Parameters(Map<String, double[][]> parameters) implements Item {
        public static Parameters deserialize(Document.Reader reader) throws IOException {
            Map<String, double[][]> parameters = new HashMap<>();
            for (int idx = 0; !reader.isEnd(); idx++) {
                Document.Reader entryReader = reader.readCompound(idx);
                String key = entryReader.readString("key");
                ArrayList<double[]> value = new ArrayList<>();
                Document.Reader valueReader = entryReader.readCompound("value");
                for (int i = 0; !valueReader.isEnd(); i++) {
                    ArrayList<Double> row = new ArrayList<>();
                    Document.Reader rowReader = valueReader.readCompound(i);
                    for (int j = 0; !rowReader.isEnd(); j++) {
                        row.add(rowReader.readFloat(j));
                    }
                    rowReader.readEnd();
                    value.add(row.stream().mapToDouble(Double::doubleValue).toArray());
                }
                parameters.put(key, value.toArray(double[][]::new));
                valueReader.readEnd();
                entryReader.readEnd();
            }
            reader.readEnd();
            return new Parameters(parameters);
        }

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            int idx = 0;
            for (Map.Entry<String, double[][]> entry : parameters.entrySet()) {
                Document.Writer entryWriter = writer.writeCompound(idx++);
                entryWriter.writeString("key", entry.getKey());
                Document.Writer valueWriter = writer.writeCompound("value");
                int i = 0;
                for (double[] l : entry.getValue()) {
                    Document.Writer rowWriter = valueWriter.writeCompound(i++);
                    int j = 0;
                    for (Double d : l) {
                        rowWriter.writeFloat(j++, d);
                    }
                    rowWriter.writeEnd();
                }
                valueWriter.writeEnd();
                entryWriter.writeEnd();
            }
            writer.writeEnd();
        }
    }

    public PredictModel(AsyncStorage storage) {
        changedFlag = 0;
        gaussMixtureModel = new GaussMixtureModel();
        entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction1.loadNGram();
        entityPrediction2 = new EntityPrediction("Categories2");
        entityPrediction2.loadNGram();
        tagPrediction = new TagPrediction("Tags.txt");

        GMModelParameters = new HashMap<>();
        this.storage = storage;
    }

    public PredictModel(Map<String, double[][]> GMModelParameters, AsyncStorage storage) {
        changedFlag = 0;
        gaussMixtureModel = new GaussMixtureModel();
        entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction1.loadNGram();
        entityPrediction2 = new EntityPrediction("Categories2");
        entityPrediction2.loadNGram();
        tagPrediction = new TagPrediction("Tags.txt");

        this.GMModelParameters = new HashMap<>();
        this.GMModelParameters.putAll(GMModelParameters);
        this.storage = storage;

    }

    public record Day(int m, int d, int w) {
        public static Day of(long time) {
            LocalDateTime utcTime = LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC);
            return new Day(utcTime.getMonthValue(), utcTime.getDayOfMonth(), utcTime.getDayOfWeek().getValue());
        }
    }

    ///  not done yet
    @Override
    public void saveParameters(AsyncStorage.ModelDirectory writer) throws IOException {
    }

    ///  not done yet
    @Override
    public void loadParameters(AsyncStorage.ModelDirectory reader) throws IOException {

        loadTransactionsAndTrain();

    }

    public void loadTransactionsAndTrain() {
        List<Pair<Double, Triple<Integer, Integer, Integer>>> transLists = new ArrayList<>();
        List<Pair<String, Pair<Double, Triple<Integer, Integer, Integer>>>> arrLists = new ArrayList<>();
        List<String> mapper = new ArrayList<>();
        GMModelParameters.clear();

        storage.transaction(table -> {
            try {
                SequencedCollection<ReferenceItemPair<Transaction>> transactions = table.list(Long.MIN_VALUE, Long.MAX_VALUE, 0, Integer.MAX_VALUE);
                for (var transaction : transactions) {
                    long time = transaction.item().time();
                    double amount = transaction.item().amount() / 100.0;
                    String category = transaction.item().category();
                    Day d = Day.of(time);

                    if (!mapper.contains(category)) {
                        mapper.add(category);
                    }

                    Pair<Double, Triple<Integer, Integer, Integer>> t = Pair.of(amount, Triple.of(d.m, d.d, d.w));
                    arrLists.add(Pair.of(category, t));
                }

                GMModelCalculation calculation = new GMModelCalculation();

                for (String category : mapper) {
                    transLists.clear();
                    for (Pair<String, Pair<Double, Triple<Integer, Integer, Integer>>> t : arrLists) {
                        if (!t.getLeft().equalsIgnoreCase(category)) {
                            continue;
                        }
                        transLists.add(t.getRight());
                    }

                    GMModelParameters.put(
                            category,
                            calculation.GMModelCalculator(transLists)
                                    .stream()
                                    .map(x -> x.stream().mapToDouble(Double::doubleValue).toArray())
                                    .toArray(double[][]::new)
                    );
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public PredictModel fork() {
        return new PredictModel(GMModelParameters, storage);
    }

    @Override
    public CompletableFuture<Void> trainOnUpdate(Update<Entity> entityUpdate, Update<Transaction> transactionUpdate) {
        if (!transactionUpdate.oldItems().isEmpty() && !transactionUpdate.newItems().isEmpty()) {
            changedFlag++;
            if (changedFlag == 2) {
                loadTransactionsAndTrain();
                changedFlag = 0;
            }
        }

        for (Map.Entry<Reference<Entity>, Entity> entry : entityUpdate.newItems().entrySet()) {
            String name = entry.getValue().name();
            String cate2 = entry.getValue().type().toString();

            entityPrediction2.setCategory(name, cate2);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>>
                    predictBudgetUsage(long reference, ImmutableLongArray time) {
        double[] budgetMean = new double[time.length()];
        double[] budgetConfidenceLower = new double[time.length()];
        double[] budgetConfidenceUpper = new double[time.length()];
        double[] montSum = new double[MONT_COUNT];

        double mean = 0.0;

        XorShift128 random = new XorShift128();

        long i = reference;
        for (int j = 0; j < time.length(); ) {
            Day curDay = Day.of(i);

            for (double[][] gm : GMModelParameters.values()) {
                gaussMixtureModel.set(gm, curDay.m, curDay.d, curDay.w);
                mean += gaussMixtureModel.getMean();

                for (int k = 0; k < MONT_COUNT; k++) {
                    montSum[k] += gaussMixtureModel.sample(random);
                }
            }


            if (i >= time.get(j)) {
                budgetMean[j] = mean;
                budgetConfidenceLower[j] = nth(montSum, montLower);
                budgetConfidenceUpper[j] = nth(montSum, montUpper);
                j++;
            }
            i += 24 * 60 * 60 * 1000L;
        }

        return CompletableFuture.completedFuture(
                ImmutablePair.of(
                        ImmutableDoubleArray.copyOf(budgetMean),
                        ImmutablePair.of(
                                ImmutableDoubleArray.copyOf(budgetConfidenceLower),
                                ImmutableDoubleArray.copyOf(budgetConfidenceUpper)
                        )
                )
        );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>>
                    predictSavedAmount(long reference, ImmutableLongArray time) {
        double[] budgetMean = new double[time.length()];
        double[] budgetConfidenceLower = new double[time.length()];
        double[] budgetConfidenceUpper = new double[time.length()];
        double[] montSum = new double[MONT_COUNT];

        double mean = 0.0;

        XorShift128 random = new XorShift128();

        for (long i = reference + 24 * 60 * 60 * 1000L, j = 0; j < time.length(); i += 24 * 60 * 60 * 1000L) {
            Day curDay = Day.of(i);
            for (Map.Entry<String, double[][]> gm : GMModelParameters.entrySet()) {
                gaussMixtureModel.set(gm.getValue(), curDay.m, curDay.d, curDay.w);
                mean += gaussMixtureModel.getMean();

                for (int k = 0; k < MONT_COUNT; k++) {
                    montSum[k] += gaussMixtureModel.sample(random);
                }
            }

            if (i >= time.get((int)j)) {
                budgetMean[(int)j] = (double) (time.get((int)j) - reference) / 120000 - mean;
                budgetConfidenceLower[(int)j] = (double) (time.get((int)j) - reference) / 130000 - nth(montSum, montUpper);
                budgetConfidenceUpper[(int)j] = (double) (time.get((int)j) - reference) / 70000 - nth(montSum, montLower);
                j++;
            }
        }

        return CompletableFuture.completedFuture(
                ImmutablePair.of(
                        ImmutableDoubleArray.copyOf(budgetMean),
                        ImmutablePair.of(
                                ImmutableDoubleArray.copyOf(budgetConfidenceLower),
                                ImmutableDoubleArray.copyOf(budgetConfidenceUpper)
                        )
                )
        );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>>
                    predictCategoriesAndTags(ImmutableList<Transaction> transactions, ImmutableList<String> categories, ImmutableList<String> tags) {
        CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>> future = new CompletableFuture<>();

        storage.entity(entity -> {
            try {
                Map<String, Integer> map1 = new HashMap<>(), map2 = new HashMap<>();

                for (int i = 0; i < categories.size(); i++) {
                    map1.put(categories.get(i).toUpperCase(), i);
                }
                for (int i = 0; i < tags.size(); i++) {
                    map2.put(tags.get(i).toUpperCase(), i);
                }

                Random random = new Random();
                int[] category = new int[transactions.size()];
                for (int i = 0; i < transactions.size(); i++) {
                    String answer = entityPrediction2.predict(transactions.get(i).title() + entity.get(transactions.get(i).entity()).name()).getLeft();
                    answer = answer.toUpperCase();

                    if (map1.containsKey(answer)) {
                        category[i] = map1.get(answer);
                    } else {
                        category[i] = random.nextInt(categories.size());
                    }
                }
                Bitmask.View2DMutable mask = Bitmask.view2DMutable(new long[Bitmask.size2d(tags.size(), transactions.size())], transactions.size());
                for (int i = 0; i < transactions.size(); i++) {
                    Day tmpDay = Day.of(transactions.get(i).time());
//                    System.out.println("new transaction date: " + tmpDay.m + " " + tmpDay.d + " " + tmpDay.w);
                    for (int j = 0; j < tags.size(); j++) {
                        mask.set(j, i, tagPrediction.checkTag(tags.get(j), tmpDay.m, tmpDay.d));
                    }
                }
                future.complete(ImmutablePair.of(
                        ImmutableIntArray.copyOf(category),
                        mask.view()
                ));
            } catch (IOException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<ImmutableIntArray> predictEntityTypes(ImmutableList<Entity> entities) {
        Map<String, Integer> map = Arrays.stream(Entity.Type.values()).collect(Collectors.toMap(Enum::name, Enum::ordinal));


        Random random = new Random();
        int entityTypesCount = Entity.Type.values().length;
        int[] types = new int[entities.size()];

        for (int i = 0; i < entities.size(); i++) {
            String answer = entityPrediction2.predict(entities.get(i).name()).getLeft();
            answer = answer.toUpperCase();

            if (map.containsKey(answer)) {
                types[i] = map.get(answer);
            } else {
                types[i] = random.nextInt(entityTypesCount);
            }
        }

        return CompletableFuture.completedFuture(
                ImmutableIntArray.copyOf(types)
        );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutablePair<Long, ImmutableLongArray>, ImmutablePair<Long, ImmutableLongArray>>>
                    predictGoals(ImmutableList<String> categories, long startTime, long endTime) {
        long[] budget = new long[categories.size()];
        long[] saving = new long[categories.size()];
        long overallBudget = 0, overallSaving = 0;
        Random random = new Random();
        for (int i = 0; i < categories.size(); i++) {
            if (GMModelParameters.containsKey(categories.get(i))) {
                budget[i] = 0;
                double curBudget = 0.0;
                for (long curTime = startTime; curTime <= endTime; curTime += 24 * 60 * 60 * 1000L) {
                    Day curDay = Day.of(curTime);
                    gaussMixtureModel.set(GMModelParameters.get(categories.get(i)), curDay.m, curDay.d, curDay.w);
                    curBudget += gaussMixtureModel.getMean();
                }
                budget[i] += (long)(curBudget * 100);
                overallBudget += budget[i];
            } else {
                saving[i] = ((endTime - startTime) / (24 * 60 * 60 * 1000L) + 1) * 20000L;
                overallSaving += saving[i];
            }
        }
        return CompletableFuture.completedFuture(
                ImmutablePair.of(
                        ImmutablePair.of(overallBudget, ImmutableLongArray.copyOf(budget)),
                        ImmutablePair.of(overallSaving, ImmutableLongArray.copyOf(saving))
                )
        );
    }

    public static double nth(double[] arr, int n) {
        return nth(arr, n, 0, arr.length - 1);
    }

    private static double nth(double[] arr, int n, int l, int r) {
        while (true) {
            int pivot = (l + r) >> 1;
            double pivotValue = arr[pivot];
            arr[pivot] = arr[r];
            int i = l, j = r - 1;
            while (i < j) {
                while (i < r && arr[i] < pivotValue) {
                    i++;
                }
                while (j > l && arr[j] > pivotValue) {
                    j--;
                }
                if (i < j) {
                    double tmp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = tmp;
                    i++;
                    j--;
                }
            }
            arr[r] = arr[i];
            arr[i] = pivotValue;
            if (i == n) {
                return arr[i];
            } else if (i < n) {
                l = i + 1;
            } else {
                r = i - 1;
            }
        }
    }
}
