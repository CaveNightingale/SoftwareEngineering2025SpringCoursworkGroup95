package io.github.software.coursework.algo.probmodel;

import io.github.software.coursework.util.XorShift128;
import org.apache.commons.math3.distribution.NormalDistribution;

public final class GaussMixtureModel {

    public double[] parameters = new double[0];
    public double[] cumulativeProbabilities = new double[0];
    public static NormalDistribution dist;
    private double mean = Double.NaN;
    private double confidenceIntervalLower = Double.NaN;
    private double confidenceIntervalUpper = Double.NaN;

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

    public double getConfidenceIntervalLower() {
        if (Double.isNaN(confidenceIntervalLower)) {
            confidenceIntervalLower = computeIntervalLower(getMean());
        }
        return confidenceIntervalLower;
    }

    public double getConfidenceIntervalUpper() {
        if (Double.isNaN(confidenceIntervalUpper)) {
            confidenceIntervalUpper = computeIntervalUpper(getMean());
        }
        return confidenceIntervalUpper;
    }

    private double computeIntegral(double l, double r) {
        double sum = 0.0;

        for (int i = 0; i + 3 <= parameters.length; i += 3) {
            sum += parameters[i + 2] * (dist.cumulativeProbability((r - parameters[i]) / parameters[i + 1])
                                  - dist.cumulativeProbability((l - parameters[i]) / parameters[i + 1]));
        }
//        System.out.println("getIntegral: " + l + ", " + r + ", sum: " + sum);

        return sum;
    }

    private double computeIntegral(double x) {
        double sum = 0.0;

        for (int i = 0; i + 3 <= parameters.length; i += 3) {
            sum += parameters[i + 2] * (dist.cumulativeProbability((x - parameters[i]) / parameters[i + 1]));
        }

        return sum;
    }

    private double computeIntervalLower(double mean) {
        double l = -9999999.0, r = mean;
        double mid = 0;
        while (r - l > 0.0000001) {
            mid = (l + r) / 2;
            if (computeIntegral(mid, mean) < 0.45) {
                r = mid;
            } else {
                l = mid;
            }
        }
        return Math.max(0, mid);
    }

    private double computeIntervalUpper(double mean) {
        double l = mean, r = 9999999.0;
        double mid = 0;
        while (r - l > 0.0000001) {
            mid = (l + r) / 2;
            if (computeIntegral(mean, mid) < 0.45) {
                l = mid;
            } else {
                r = mid;
            }
        }
        return Math.max(0, mid);
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
