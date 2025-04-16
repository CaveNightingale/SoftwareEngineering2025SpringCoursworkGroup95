package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.Bitmask;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * A model that does nothing. (Random guessing in classification, and arbitrary hard-coded linear function in regression)
 */
public final class NoSkill implements Model {
    @Override
    public void saveParameters(AsyncStorage.ModelDirectory writer) throws IOException {
    }

    @Override
    public void loadParameters(AsyncStorage.ModelDirectory reader) throws IOException {
    }

    @Override
    public NoSkill fork() {
        return new NoSkill();
    }

    @Override
    public CompletableFuture<Void> trainOnUpdate(Update<Entity> entityUpdate, Update<Transaction> transactionUpdate) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> predictBudgetUsage(long reference, ImmutableLongArray time) {
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

    @Override
    public CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>> predictSavedAmount(long reference, ImmutableLongArray time) {
        double[] budgetMean = new double[time.length()];
        double[] budgetConfidenceLower = new double[time.length()];
        double[] budgetConfidenceUpper = new double[time.length()];
        for (int i = 0; i < time.length(); i++) {
            budgetMean[i] = (double) (time.get(i) - reference) / 120000;
            budgetConfidenceLower[i] = (double) (time.get(i) - reference) / 110000;
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
    public CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>> predictCategoriesAndTags(ImmutableList<Transaction> transactions, ImmutableList<String> categories, ImmutableList<String> tags) {
        Random random = new Random();
        int[] category = new int[transactions.size()];
        for (int i = 0; i < transactions.size(); i++) {
            category[i] = random.nextInt(categories.size());
        }
        Bitmask.View2DMutable mask = Bitmask.view2DMutable(new long[Bitmask.size2d(tags.size(), transactions.size())], transactions.size());
        for (int i = 0; i < tags.size(); i++) {
            for (int j = 0; j < transactions.size(); j++) {
                mask.set(i, j, random.nextBoolean());
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
        Random random = new Random();
        int entityTypesCount = Entity.Type.values().length;
        int[] types = new int[entities.size()];
        for (int i = 0; i < entities.size(); i++) {
            types[i] = random.nextInt(entityTypesCount);
        }
        return CompletableFuture.completedFuture(
                ImmutableIntArray.copyOf(types)
        );
    }

    @Override
    public CompletableFuture<ImmutablePair<ImmutablePair<Long, ImmutableLongArray>, ImmutablePair<Long, ImmutableLongArray>>> predictGoals(ImmutableList<String> categories, long startTime, long endTime) {
        long[] budget = new long[categories.size()];
        long[] saving = new long[categories.size()];
        Random random = new Random();
        for (int i = 0; i < categories.size(); i++) {
            budget[i] = random.nextInt(10000);
            saving[i] = random.nextInt(20000);
        }
        return CompletableFuture.completedFuture(
                ImmutablePair.of(
                        ImmutablePair.of(random.nextLong(1000L), ImmutableLongArray.copyOf(budget)),
                        ImmutablePair.of(random.nextLong(2000L), ImmutableLongArray.copyOf(saving))
                )
        );
    }
}
