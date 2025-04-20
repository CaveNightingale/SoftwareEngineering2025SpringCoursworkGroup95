package io.github.software.coursework;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.*;

public class GMModelGenerator {

    public static class GMMComponent {
        public double mu;
        public double sigma;
        public List<Double> F;
        public List<Double> G;
        public List<Double> H;

        public GMMComponent(double mu, double sigma, List<Double> F, List<Double> G, List<Double> H) {
            this.mu = mu;
            this.sigma = sigma;
            this.F = F;
            this.G = G;
            this.H = H;
        }
    }

    private final Random rand = new Random();
    private int componentCount;
    private List<GMMComponent> components;
    private List<List<Double>> answerGMModel;

    public GMModelGenerator() {
        generateComponents();
    }

    private void generateComponents() {
        componentCount = rand.nextInt(21) + 10;  // 10~30
        components = new ArrayList<>();
        answerGMModel = new ArrayList<>();

        List<Double>[] F = new List[12];
        List<Double>[] G = new List[31];
        List<Double>[] H = new List[7];

        for (int i = 0; i < 12; i++) F[i] = randomDistribution(componentCount);
        for (int i = 0; i < 31; i++) G[i] = randomDistribution(componentCount);
        for (int i = 0; i < 7; i++) H[i] = randomDistribution(componentCount);

        for (int i = 0; i < componentCount; i++) {
            double mu = 10 + rand.nextDouble() * 90;
            double sigma = 1 + rand.nextDouble() * 10;

            List<Double> f = new ArrayList<>();
            List<Double> g = new ArrayList<>();
            List<Double> h = new ArrayList<>();

            List<Double> full = new ArrayList<>();
            full.add(mu);
            full.add(sigma);

            for (int j = 0; j < 12; j++) {
                double v = F[j].get(i);
                f.add(v);
                full.add(v);
            }

            for (int j = 0; j < 31; j++) {
                double v = G[j].get(i);
                g.add(v);
                full.add(v);
            }

            for (int j = 0; j < 7; j++) {
                double v = H[j].get(i);
                h.add(v);
                full.add(v);
            }

            components.add(new GMMComponent(mu, sigma, f, g, h));
            answerGMModel.add(full);
        }
    }

    private List<Double> randomDistribution(int size) {
        List<Double> raw = new ArrayList<>();
        for (int i = 0; i < size; i++) raw.add(rand.nextDouble());
        double sum = raw.stream().mapToDouble(Double::doubleValue).sum();

        List<Double> norm = new ArrayList<>();
        for (double d : raw) norm.add(d / sum / 3.0); // 权重和约为 1/3
        return norm;
    }

    public List<Pair<Double, Triple<Integer, Integer, Integer>>> generateData(int samples) {
        List<Pair<Double, Triple<Integer, Integer, Integer>>> data = new ArrayList<>();

        for (int i = 0; i < samples; i++) {
            GMMComponent comp = components.get(rand.nextInt(componentCount));
            int month = rand.nextInt(12) + 1;
            int day = rand.nextInt(31) + 1;
            int weekday = rand.nextInt(7) + 1;

            double y = comp.mu + comp.sigma * rand.nextGaussian();
            data.add(new ImmutablePair<>(y, new ImmutableTriple<>(month, day, weekday)));
        }

        return data;
    }

    public List<List<Double>> getAnswerGMModel() {
        return answerGMModel;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public List<GMMComponent> getComponents() {
        return components;
    }
}
