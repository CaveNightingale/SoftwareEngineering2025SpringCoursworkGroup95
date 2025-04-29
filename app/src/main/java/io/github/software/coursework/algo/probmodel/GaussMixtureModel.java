package io.github.software.coursework.algo.probmodel;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;

public class GaussMixtureModel {

    public List<Triple<Double, Double, Double>> parameters;
    public static NormalDistribution dist;

    public GaussMixtureModel() {
        parameters = new ArrayList<>();
        dist = new NormalDistribution(0, 1);
    }

    public void set(List<List<Double>> GMModelParam, int m, int d, int w) {
        int month = 1 + m;
        int day = 13 + d;
        int week = 44 + w;

        for (List<Double> param : GMModelParam) {
            if (param.size() != 52) {
                throw new IllegalArgumentException("GMModelParam must have exactly 52 values");
            }
        }

        parameters.clear();

        for (List<Double> param : GMModelParam) {
            parameters.add(Triple.of(param.get(0), param.get(1), param.get(month) + param.get(day) + param.get(week)));
        }
    }

    public double getMean() {
        double sum = 0.0;
        for (Triple<Double, Double, Double> param : parameters) {
            sum += param.getLeft() * param.getRight();
        }

        if (sum < 0.0)
            sum = 0.0;

        return sum;
    }

    public double getIntegral(double l, double r) {
        double sum = 0.0;

        for (Triple<Double, Double, Double> param : parameters) {
            sum += param.getRight() * (dist.cumulativeProbability((r - param.getLeft()) / param.getMiddle())
                                     - dist.cumulativeProbability((l - param.getLeft()) / param.getMiddle()));
        }

        System.out.println("getIntegral: " + l + ", " + r + ", sum: " + sum);

        return sum;
    }

    public Pair<Double, Double> getInterval() {
        double mean = getMean();
        double l = -9999999.0, r = mean;
        double reL, reR, mid = 0;
        while (r - l > 0.0000001) {
            mid = (l + r) / 2;
            if (getIntegral(mid, mean) < 0.45) {
                r = mid;
            } else {
                l = mid;
            }
        }

        reL = mid;
        l = mean;
        r = 9999999.0;
        while (r - l > 0.0000001) {
            mid = (l + r) / 2;
            if (getIntegral(mean, mid) < 0.45) {
                l = mid;
            } else {
                r = mid;
            }
        }
        reR = mid;

//        if (reR > mean * 2) reR = mean * 2;
//        if (reR < mean) reR = mean;
//        if (reL < mean * 0.5) reL = mean * 0.5;
//        if (reL > mean) reL = mean;

        return new ImmutablePair<>(Math.max(0, reL), Math.max(0, reR));
    }

    public Pair<Double, Pair<Double, Double>> getMeanAndInterval() {
        return new ImmutablePair<>(getMean(), getInterval());
    }
}
