package io.github.software.coursework.ProbabilityModel;

import java.util.Random;

public class LogNormalModel implements DistributionModel {
    private final double mu;
    private final double sigma;
    private final Random random = new Random();

    public LogNormalModel(double mu, double sigma) {
        this.mu = mu;
        this.sigma = sigma;
    }

    @Override
    public double generateAmount() {
        return Math.max(0, Math.exp(mu + sigma * random.nextGaussian()));
    }
}
