package io.github.software.coursework.ProbabilityModel;

import java.util.Random;

public class NormalModel implements DistributionModel {
    private final double mean;
    private final double stddev;

    public NormalModel(double mean, double stddev) {
        this.mean = mean;
        this.stddev = stddev;
    }

    @Override
    public double generateAmount(Random random) {
        return Math.max(0, mean + stddev * random.nextGaussian());
    }
}
