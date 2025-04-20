package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.EntityPrediction;
import io.github.software.coursework.ProbabilityModel.GaussMixtureModel;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.Bitmask;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class PredictModel implements Model {

    public static GaussMixtureModel gaussMixtureModel;
    public static EntityPrediction entityPrediction1;
    public static EntityPrediction entityPrediction2;
    public static List<List<Double>> GMModelParameters;

    public PredictModel() {
        gaussMixtureModel = new GaussMixtureModel();
        entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction2 = new EntityPrediction("Categories2");

        GMModelParameters = new ArrayList<>();
    }

    public PredictModel(List<List<Double>> GMModelParameters) {
        gaussMixtureModel = new GaussMixtureModel();
        entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction2 = new EntityPrediction("Categories2");

        this.GMModelParameters = new ArrayList<>();
        for (List<Double> gm : GMModelParameters) {
            GMModelParameters.add(new ArrayList<>(gm));
        }
    }

    ///  not done yet
    @Override
    public void saveParameters(AsyncStorage.ModelDirectory writer) throws IOException {
    }

    ///  not done yet
    @Override
    public void loadParameters(AsyncStorage.ModelDirectory reader) throws IOException {

    }

    @Override
    public PredictModel fork() {
        return new PredictModel(GMModelParameters);
    }

    ///  not done yet
    @Override
    public CompletableFuture<Void> trainOnUpdate(Update<Entity> entityUpdate, Update<Transaction> transactionUpdate) {
        return CompletableFuture.completedFuture(null);
    }

    ///  not done yet
    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>>
                    predictBudgetUsage(long reference, ImmutableLongArray time) {
        double[] budgetMean = new double[time.length()];
        double[] budgetConfidenceLower = new double[time.length()];
        double[] budgetConfidenceUpper = new double[time.length()];
        for (int i = 0; i < time.length(); i++) {
            budgetMean[i] = (double) (time.get(i) - reference) / 85000;
            budgetConfidenceLower[i] = (double) (time.get(i) - reference) / 100000;
            budgetConfidenceUpper[i] = (double) (time.get(i) - reference) / 60000;
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

    ///  not done yet
    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> predictSavedAmount(long reference, ImmutableLongArray time) {
        double[] budgetMean = new double[time.length()];
        double[] budgetConfidenceLower = new double[time.length()];
        double[] budgetConfidenceUpper = new double[time.length()];
        for (int i = 0; i < time.length(); i++) {
            budgetMean[i] = (double) (time.get(i) - reference) / 120000;
            budgetConfidenceLower[i] = (double) (time.get(i) - reference) / 130000;
            budgetConfidenceUpper[i] = (double) (time.get(i) - reference) / 70000;
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
                    predictCategoriesAndTags(ImmutableList<Transaction> transactions, ImmutableList<String> categories, ImmutableList<String> tags)
                                    throws IllegalArgumentException {
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
            String answer = entityPrediction2.predict(transactions.get(i).title() + transactions.get(i).entity().getClass().getName()).getLeft();
            answer = answer.toUpperCase();

            if (map1.containsKey(answer)) {
                category[i] = map1.get(answer);
            } else {
                category[i] = random.nextInt(categories.size());
            }
        }
        Bitmask.View2DMutable mask = Bitmask.view2DMutable(new long[Bitmask.size2d(tags.size(), transactions.size())], transactions.size());
        for (int i = 0; i < transactions.size(); i++) {
            for (int j = 0; j < tags.size(); j++) {
                mask.set(j, i, false);
            }

            String answer = entityPrediction1.predict(transactions.get(i).title() + transactions.get(i).entity().getClass().getName()).getLeft();
            answer = answer.toUpperCase();

            if (map2.containsKey(answer)) {
                mask.set(map2.get(answer), i, true);
            }
        }

        return CompletableFuture.completedFuture(
                ImmutablePair.of(
                        ImmutableIntArray.copyOf(category),
                        mask.view()
                )
        );
    }

    @Override
    public CompletableFuture<ImmutableIntArray> predictEntityTypes(ImmutableList<Entity> entities) {
        Map<String, Integer> map = new HashMap<>();
        map.put("UNKNOWN", 0);
        map.put("INDIVIDUAL", 1);
        map.put("EDUCATION", 2);
        map.put("GOVERNMENT", 3);
        map.put("COMMERCIAL", 4);
        map.put("NONPROFIT", 5);

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

    ///  not done yet
    @Override
    public CompletableFuture<ImmutablePair<ImmutablePair<Long, ImmutableLongArray>, ImmutablePair<Long, ImmutableLongArray>>> predictGoals(ImmutableList<String> categories, long startTime, long endTime) {
        long[] budget = new long[categories.size()];
        long[] saving = new long[categories.size()];
        Random random = new Random();
        for (int i = 0; i < categories.size(); i++) {
            if (random.nextBoolean()) {
                budget[i] = random.nextLong(1000L);
            } else {
                saving[i] = random.nextLong(1000L);
            }
        }
        return CompletableFuture.completedFuture(
                ImmutablePair.of(
                        ImmutablePair.of(random.nextLong(1000L), ImmutableLongArray.copyOf(budget)),
                        ImmutablePair.of(random.nextLong(2000L), ImmutableLongArray.copyOf(saving))
                )
        );
    }
}
