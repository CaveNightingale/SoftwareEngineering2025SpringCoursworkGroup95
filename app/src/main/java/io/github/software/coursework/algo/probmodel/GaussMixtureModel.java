package io.github.software.coursework.algo.probmodel;

import io.github.software.coursework.util.XorShift128;
import org.apache.commons.math3.distribution.NormalDistribution;

public final class GaussMixtureModel {

    public double[] parameters = new double[0];
    public double[] cumulativeProbabilities = new double[0];
    public static NormalDistribution dist;
    private double mean = Double.NaN;

    public GaussMixtureModel() {
        dist = new NormalDistribution(0, 1);
    }

    public void set(double[][] GMModelParam, int m, int d, int w) {
        int month = 1 + m;
        int day = 13 + d;
        int week = 44 + w;

        for (double[] param : GMModelParam) {
            if (param.length != 52) {
                throw new IllegalArgumentException("GMModelParam must have exactly 52 values");
            }
        }

        // Parameters: mean, stdDev, weight
        parameters = new double[GMModelParam.length * 3];
        cumulativeProbabilities = new double[GMModelParam.length];
        mean = 0;
        double cumulative = 0;

        for (int i = 0; i < GMModelParam.length; i++) {
            parameters[i * 3] = GMModelParam[i][0];
            parameters[i * 3 + 1] = GMModelParam[i][1];
            parameters[i * 3 + 2] = GMModelParam[i][month] + GMModelParam[i][day] + GMModelParam[i][week];
            mean += parameters[i * 3] * parameters[i * 3 + 2];
            cumulativeProbabilities[i] = cumulative;
            cumulative += parameters[i * 3 + 2];
        }

        if (mean < 0.0) {
            mean = 0.0;
        }
    }

    public double getMean() {
        return mean;
    }

    public double sample(XorShift128 randomGenerator) {
        double random = randomGenerator.nextDouble();
        int l = 0, r = cumulativeProbabilities.length - 1;
        while (r > l) {
            int mid = (l + r + 1) / 2;
            if (cumulativeProbabilities[mid] > random) {
                r = mid - 1;
            } else {
                l = mid;
            }
        }
        double mean = parameters[l * 3];
        double stdDev = parameters[l * 3 + 1];
        return randomGenerator.nextGaussian() * stdDev + mean;
    }
}
