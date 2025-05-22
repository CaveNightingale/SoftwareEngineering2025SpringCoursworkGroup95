package io.github.software.coursework.ProbabilityModel;

import java.util.Random;

public class LogNormalModel implements DistributionModel {
    private final double mu;
    private final double sigma;

    public LogNormalModel(double mu, double sigma) {
        this.mu = mu;
        this.sigma = sigma;
    }

    @Override
    public double generateAmount(Random random) {
        return Math.max(0, Math.exp(mu + sigma * random.nextGaussian()));
    }
}
