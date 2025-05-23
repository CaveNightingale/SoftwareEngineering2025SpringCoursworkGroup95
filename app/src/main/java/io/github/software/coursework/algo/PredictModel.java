package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.primitives.ImmutableLongArray;
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
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static org.apache.commons.lang3.math.NumberUtils.max;

public final class PredictModel implements Model {

    public static final int MONT_COUNT = 1024;
    private static final int montLower = (int) (0.05 * MONT_COUNT);
    private static final int montUpper = (int) (0.95 * MONT_COUNT);

    public EntityPrediction entityPrediction1;
    public EntityPrediction entityPrediction2;
    public Map<String, double[][]> gMModelBudgetParameters;
    public Map<String, double[][]> gMModelSaveParameters;
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
        entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction1.loadNGram();
        entityPrediction2 = new EntityPrediction("Categories2");
        entityPrediction2.loadNGram();
        tagPrediction = new TagPrediction("Tags.txt");

        gMModelBudgetParameters = new HashMap<>();
        gMModelSaveParameters = new HashMap<>();
        this.storage = storage;
    }

    public PredictModel(Map<String, double[][]> gMModelBudgetParameters, Map<String, double[][]> gMModelSaveParameters, AsyncStorage storage) {
        changedFlag = 0;
        entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction1.loadNGram();
        entityPrediction2 = new EntityPrediction("Categories2");
        entityPrediction2.loadNGram();
        tagPrediction = new TagPrediction("Tags.txt");

        this.gMModelBudgetParameters = new HashMap<>();
        this.gMModelSaveParameters = new HashMap<>();
        this.gMModelBudgetParameters.putAll(gMModelBudgetParameters);
        this.gMModelSaveParameters.putAll(gMModelSaveParameters);
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
        gMModelBudgetParameters.clear();
        gMModelSaveParameters.clear();

        CompletableFuture<Void> loaded = new CompletableFuture<>();

        storage.transaction(table -> {
            try {
                List<Pair<String, Pair<Double, Triple<Integer, Integer, Integer>>>> arrLists = new ArrayList<>();
                List<String> mapper = new ArrayList<>();
                SequencedCollection<ReferenceItemPair<Transaction>> transactions = table.list(Long.MIN_VALUE, Long.MAX_VALUE, 0, Integer.MAX_VALUE);
                for (var transaction : transactions) {
                    long time = transaction.item().time();
                    double amount = transaction.item().amount() / 100.0;
                    String category = transaction.item().category();
                    if (category == null) {
                        category = "Diet";
                    }

                    Day d = Day.of(time);

                    if (!mapper.contains(category)) {
                        mapper.add(category);
                    }

                    Pair<Double, Triple<Integer, Integer, Integer>> t = Pair.of(amount, Triple.of(d.m, d.d, d.w));
                    arrLists.add(Pair.of(category, t));
                }

                GMModelCalculation calculation = new GMModelCalculation();

                for (String category : mapper) {
                    List<Pair<Double, Triple<Integer, Integer, Integer>>> transListsSave = new ArrayList<>();
                    List<Pair<Double, Triple<Integer, Integer, Integer>>> transListsBudget = new ArrayList<>();

                    for (Pair<String, Pair<Double, Triple<Integer, Integer, Integer>>> t : arrLists) {
                        if ((!t.getLeft().equalsIgnoreCase(category)) || t.getRight().getLeft() == 0.0) {
                            continue;
                        }
                        if (t.getRight().getLeft() > 0)
                            transListsSave.add(t.getRight());
                        else
                            transListsBudget.add(Pair.of(-t.getRight().getLeft(), t.getRight().getRight()));
                    }

                    this.gMModelSaveParameters.put(
                            category,
                            calculation.GMModelCalculator(transListsSave)
                                    .stream()
                                    .map(x -> x.stream().mapToDouble(Double::doubleValue).toArray())
                                    .toArray(double[][]::new)
                    );
                    this.gMModelBudgetParameters.put(
                            category,
                            calculation.GMModelCalculator(transListsBudget)
                                    .stream()
                                    .map(x -> x.stream().mapToDouble(Double::doubleValue).toArray())
                                    .toArray(double[][]::new)
                    );
                    loaded.complete(null);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            loaded.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PredictModel fork() {
        return new PredictModel(gMModelBudgetParameters, gMModelSaveParameters, storage);
    }

    @Override
    public CompletableFuture<Void> trainOnUpdate(Update<Entity> entityUpdate, Update<Transaction> transactionUpdate) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        storage.model(entity -> {
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
            future.complete(null);
        });
        return future;
    }

    public ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>
                    predictBudgetUsageAsync(long reference, ImmutableLongArray time) {

        GaussMixtureModel gaussMixtureModel = new GaussMixtureModel();
        double[] budgetMean = new double[time.length()];
        double[] budgetConfidenceLower = new double[time.length()];
        double[] budgetConfidenceUpper = new double[time.length()];
        double[] montSum = new double[MONT_COUNT];

        double mean = 0.0;

        XorShift128 random = new XorShift128();

        long i = reference;
        for (int j = 0; j < time.length(); ) {
            Day curDay = Day.of(i);

            for (double[][] gm : gMModelBudgetParameters.values()) {

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

                if (budgetMean[j] < 0)
                    budgetMean[j] = 0;
                if (budgetConfidenceLower[j] < 0)
                    budgetConfidenceLower[j] = 0;
                if (budgetConfidenceUpper[j] < 0)
                    budgetConfidenceUpper[j] = 0;

                j++;
            }
            i += 24 * 60 * 60 * 1000L;
        }

        return ImmutablePair.of(
                        ImmutableDoubleArray.copyOf(budgetMean),
                        ImmutablePair.of(
                                ImmutableDoubleArray.copyOf(budgetConfidenceLower),
                                ImmutableDoubleArray.copyOf(budgetConfidenceUpper)
                        )
                );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>>
                    predictBudgetUsage(long reference, ImmutableLongArray time) {
        CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> future = new CompletableFuture<>();
        storage.model(ignored -> future.complete(predictBudgetUsageAsync(reference, time)));
        return future;
    }

    public ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>
                    predictSavedAmountAsync(long reference, ImmutableLongArray time) {
        GaussMixtureModel gaussMixtureModel = new GaussMixtureModel();
        double[] savingMean = new double[time.length()];
        double[] savingConfidenceLower = new double[time.length()];
        double[] savingConfidenceUpper = new double[time.length()];
        double[] montSum2 = new double[MONT_COUNT];

        double mean1 = 0.0;

        XorShift128 random1 = new XorShift128();

        for (long i = reference, j = 0; j < time.length(); i += 24 * 60 * 60 * 1000L) {
            Day curDay = Day.of(i);

            for (double[][] gm : gMModelSaveParameters.values()) {

                gaussMixtureModel.set(gm, curDay.m, curDay.d, curDay.w);

                mean1 += gaussMixtureModel.getMean();

                for (int k = 0; k < MONT_COUNT; k++) {
                    montSum2[k] += gaussMixtureModel.sample(random1);
                }
            }
            for (double[][] gm : gMModelBudgetParameters.values()) {

                gaussMixtureModel.set(gm, curDay.m, curDay.d, curDay.w);

                mean1 -= gaussMixtureModel.getMean();

                for (int k = 0; k < MONT_COUNT; k++) {
                    montSum2[k] -= gaussMixtureModel.sample(random1);
                }

            }
            if (i >= time.get((int)j)) {
                savingMean[(int)j] = mean1;
                savingConfidenceLower[(int)j] = nth(montSum2, montLower);
                savingConfidenceUpper[(int)j] = nth(montSum2, montUpper);
                j++;
            }

        }

        return ImmutablePair.of(
                        ImmutableDoubleArray.copyOf(savingMean),
                        ImmutablePair.of(
                                ImmutableDoubleArray.copyOf(savingConfidenceLower),
                                ImmutableDoubleArray.copyOf(savingConfidenceUpper)
                        )
                );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>>
                    predictSavedAmount(long reference, ImmutableLongArray time) {
        CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> future = new CompletableFuture<>();
        storage.model(ignored -> future.complete(predictSavedAmountAsync(reference, time)));
        return future;
    }

    public ImmutablePair<ImmutableIntArray, Bitmask.View2D>
                    predictCategoriesAndTagsAsync(ImmutableList<Transaction> transactions, ImmutableList<String> categories, ImmutableList<String> tags) throws ExecutionException, InterruptedException {
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

        return future.get();
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>>
                    predictCategoriesAndTags(ImmutableList<Transaction> transactions, ImmutableList<String> categories, ImmutableList<String> tags) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return predictCategoriesAndTagsAsync(transactions, categories, tags);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ImmutableIntArray predictEntityTypesAsync(ImmutableList<Entity> entities) {
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

        return ImmutableIntArray.copyOf(types);
    }

    @Override
    public CompletableFuture<ImmutableIntArray> predictEntityTypes(ImmutableList<Entity> entities) {
        CompletableFuture<ImmutableIntArray> future = new CompletableFuture<>();
        storage.model(ignored -> future.complete(predictEntityTypesAsync(entities)));
        return future;
    }

    public ImmutablePair<ImmutablePair<Long, ImmutableLongArray>, ImmutablePair<Long, ImmutableLongArray>>
                    predictGoalsAsync(ImmutableList<String> categories, long startTime, long endTime) {

        GaussMixtureModel gaussMixtureModel = new GaussMixtureModel();
        long[] budget = new long[categories.size()];
        long[] saving = new long[categories.size()];
        long overallBudget = 0, overallSaving = 0;
        Random random = new Random();
        for (int i = 0; i < categories.size(); i++) {
            if (gMModelBudgetParameters.containsKey(categories.get(i))) {
                budget[i] = 0;
                double curBudget = 0.0;
                for (long curTime = startTime; curTime <= endTime; curTime += 24 * 60 * 60 * 1000L) {
                    Day curDay = Day.of(curTime);
                    gaussMixtureModel.set(gMModelBudgetParameters.get(categories.get(i)), curDay.m, curDay.d, curDay.w);
                    curBudget += gaussMixtureModel.getMean();
                }
                budget[i] += (long)(curBudget * 100);
                overallBudget += budget[i];
            } else {
                saving[i] = 0;
                double curSaving = 0.0;
                for (long curTime = startTime; curTime <= endTime; curTime += 24 * 60 * 60 * 1000L) {
                    Day curDay = Day.of(curTime);
                    gaussMixtureModel.set(gMModelSaveParameters.get(categories.get(i)), curDay.m, curDay.d, curDay.w);
                    curSaving += gaussMixtureModel.getMean();
                }
                saving[i] += (long)(curSaving * 100);
                overallSaving += saving[i];
            }
        }
        return ImmutablePair.of(
                        ImmutablePair.of(overallBudget, ImmutableLongArray.copyOf(budget)),
                        ImmutablePair.of(overallSaving, ImmutableLongArray.copyOf(saving))
                );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutablePair<Long, ImmutableLongArray>, ImmutablePair<Long, ImmutableLongArray>>>
                    predictGoals(ImmutableList<String> categories, long startTime, long endTime) {
        CompletableFuture<ImmutablePair<ImmutablePair<Long, ImmutableLongArray>, ImmutablePair<Long, ImmutableLongArray>>> future = new CompletableFuture<>();
        storage.model(ignored -> future.complete(predictGoalsAsync(categories, startTime, endTime)));
        return future;
    }

    private static double nth(double[] arr, int n) {
        return nth(arr, n, 0, arr.length - 1);
    }

    /*
     * Linear selection algorithm.
     * It selects the midpoint as the principal component because arr is roughly ordered
     */
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
