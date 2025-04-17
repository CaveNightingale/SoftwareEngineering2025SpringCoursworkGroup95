package io.github.software.coursework;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.*;

public class GMModelCalculationTest {

    static class GMMComponent {
        double mu;
        double sigma;
        List<Double> F;  // size 12
        List<Double> G;  // size 31
        List<Double> H;  // size 7

        public GMMComponent(double mu, double sigma, List<Double> F, List<Double> G, List<Double> H) {
            this.mu = mu;
            this.sigma = sigma;
            this.F = F;
            this.G = G;
            this.H = H;
        }
    }

    public static void main(String[] args) {
        Random rand = new Random();
        int componentCount = rand.nextInt(21) + 10;  // 10~30
        List<GMMComponent> components = new ArrayList<>();
        for (int i = 0; i < componentCount; i++) {
            double mu = 10 + rand.nextDouble() * 90; // 10~100 RMB
            double sigma = 1 + rand.nextDouble() * 10;

            List<Double> F = randomDistribution(rand, 12);
            List<Double> G = randomDistribution(rand, 31);
            List<Double> H = randomDistribution(rand, 7);

            components.add(new GMMComponent(mu, sigma, F, G, H));
        }

        //
        List<Pair<Double, Triple<Integer, Integer, Integer>>> data = new ArrayList<>();
        int samples = 10000;

        for (int i = 0; i < samples; i++) {
            GMMComponent comp = components.get(rand.nextInt(componentCount));
            int month = rand.nextInt(12) + 1;
            int day = rand.nextInt(31) + 1;
            int weekday = rand.nextInt(7) + 1;

            double weight = comp.F.get(month - 1) + comp.G.get(day - 1) + comp.H.get(weekday - 1);
            double y = comp.mu + comp.sigma * rand.nextGaussian();

            data.add(new ImmutablePair<>(y, new ImmutableTriple<>(month, day, weekday)));
        }

        GMModelCalculation g = new GMModelCalculation();
        //
        List<List<Double>> modelOutput = g.GMModelCalculator(data);

        //
        double bicScore = g.BICCalculator(modelOutput);

        System.out.println("Generated component count: " + componentCount);
        System.out.println("Fitted component count: " + modelOutput.size());
        System.out.println("Fitted Model BIC: " + bicScore);
    }

    //
    private static List<Double> randomDistribution(Random rand, int size) {
        List<Double> raw = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            raw.add(rand.nextDouble());
        }
        double sum = raw.stream().mapToDouble(Double::doubleValue).sum();
        List<Double> norm = new ArrayList<>();
        for (double d : raw) {
            norm.add(d / sum / 3.0); //
        }
        return norm;
    }
}
