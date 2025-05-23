package io.github.software.coursework.gui;

import com.google.common.primitives.ImmutableDoubleArray;

public final class ChartSequentialPredictionModel {

    private final ImmutableDoubleArray trainingSamples;
    private final ImmutableDoubleArray predictedMean;
    private final ImmutableDoubleArray predictedLowerBound;
    private final ImmutableDoubleArray predictedUpperBound;
    private final long start;
    private final long end;
    private final long today;
    private final double reference;
    private final String referenceName;

    private final int sequenceLength;
    private final double top;
    private final double bottom;

    public ChartSequentialPredictionModel(
            ImmutableDoubleArray trainingSamples,
            ImmutableDoubleArray predictedMean,
            ImmutableDoubleArray predictedLowerBound,
            ImmutableDoubleArray predictedUpperBound,
            long start,
            long end,
            long today,
            double reference,
            String referenceName
    ) {
        this.trainingSamples = trainingSamples;
        this.predictedMean = predictedMean;
        this.predictedLowerBound = predictedLowerBound;
        this.predictedUpperBound = predictedUpperBound;
        this.start = start;
        this.end = end;
        this.today = today;
        this.reference = reference;
        this.referenceName = referenceName;

        this.sequenceLength = trainingSamples.length() + predictedMean.length();
        double top = 1;
        double bottom = 0;

        if (Double.isFinite(reference)) {
            top = Math.max(top, reference);
            bottom = Math.min(bottom, reference);
        }
        double predictBase = trainingSamples.isEmpty() ? 0 : trainingSamples.get(trainingSamples.length() - 1);
        for (int i = 0; i < predictedUpperBound.length(); i++) {
            top = Math.max(top, predictedUpperBound.get(i) + predictBase);
            bottom = Math.min(bottom, predictedUpperBound.get(i) + predictBase);
        }
        for (int i = 0; i < predictedLowerBound.length(); i++) {
            top = Math.max(top, predictedLowerBound.get(i) + predictBase);
            bottom = Math.min(bottom, predictedLowerBound.get(i) + predictBase);
        }
        for (int i = 0; i < predictedMean.length(); i++) {
            top = Math.max(top, predictedMean.get(i) + predictBase);
            bottom = Math.min(bottom, predictedMean.get(i) + predictBase);
        }
        for (int i = 0; i < trainingSamples.length(); i++) {
            top = Math.max(top, trainingSamples.get(i));
            bottom = Math.min(bottom, trainingSamples.get(i));
        }
        this.top = top;
        this.bottom = bottom;
    }

    public ImmutableDoubleArray getTrainingSamples() {
        return trainingSamples;
    }

    public ImmutableDoubleArray getPredictedMean() {
        return predictedMean;
    }

    public ImmutableDoubleArray getPredictedLowerBound() {
        return predictedLowerBound;
    }

    public ImmutableDoubleArray getPredictedUpperBound() {
        return predictedUpperBound;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getToday() {
        return today;
    }

    public double getReference() {
        return reference;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public double getTop() {
        return top;
    }

    public double getBottom() {
        return bottom;
    }
}
