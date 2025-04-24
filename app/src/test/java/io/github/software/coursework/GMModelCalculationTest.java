package io.github.software.coursework;

import io.github.software.coursework.algo.probmodel.GMModelCalculation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.Test;

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

    @Test
    public void gMModedlCalculationTest() {
        Random rand = new Random();
        int componentCount = rand.nextInt(21) + 10;  // 10~30
        List<GMMComponent> components = new ArrayList<>();

        List<Double>[] F = new List[12];
        List<Double>[] G = new List[31];
        List<Double>[] H = new List[7];

        List<Double> tmpList;

        for (int i = 0; i < 12; i++) {
            F[i] = randomDistribution(rand, componentCount);
        }
        for (int i = 0; i < 31; i++) {
            G[i] = randomDistribution(rand, componentCount);

        }
        for (int i = 0; i < 7; i++) {
            H[i] = randomDistribution(rand, componentCount);

        }

        List<List<Double>> answerGMModel = new ArrayList<>();

        for (int i = 0; i < componentCount; i++) {
            List<Double> tmp = new ArrayList<>();

            double mu = 10 + rand.nextDouble() * 90; // 10~100 RMB
            double sigma = 1 + rand.nextDouble() * 10;

            tmp.add(mu);
            tmp.add(sigma);

            List<Double> f = new ArrayList<>();
            List<Double> g = new ArrayList<>();
            List<Double> h = new ArrayList<>();

            for (int j = 0; j < 12; j++) {
                f.add(F[j].get(i));

                tmp.add(F[j].get(i));
            }
            for (int j = 0; j < 31; j++) {
                g.add(G[j].get(i));

                tmp.add(G[j].get(i));
            }
            for (int j = 0; j < 7; j++) {
                h.add(H[j].get(i));

                tmp.add(H[j].get(i));
            }

            System.out.println("size = " + tmp.size());

            answerGMModel.add(tmp);

            components.add(new GMMComponent(mu, sigma, f, g, h));
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
        System.out.println("Fitted Model LicklyHood: " + (bicScore - Math.log(samples) * (modelOutput.size() * 3 - 1)));

        Double AnsBICScore = g.BICCalculator(answerGMModel);
        System.out.println("Ans Model LicklyHood: " + (AnsBICScore - Math.log(samples) * (answerGMModel.size() * 3 - 1)));
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
