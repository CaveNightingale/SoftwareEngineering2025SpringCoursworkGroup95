package io.github.software.coursework.algo.probmodel;

import java.util.Random;

public class NormalModel implements DistributionModel {
    private final double mean;
    private final double stddev;
    private final Random random = new Random();

    public NormalModel(double mean, double stddev) {
        this.mean = mean;
        this.stddev = stddev;
    }

    @Override
    public double generateAmount() {
        return Math.max(0, mean + stddev * random.nextGaussian());
    }
}
