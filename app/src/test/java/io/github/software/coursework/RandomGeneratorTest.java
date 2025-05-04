package io.github.software.coursework;

import io.github.software.coursework.util.XorShift128;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class RandomGeneratorTest {
    private static final int cdfSampleSize = 1000000;
    private static final int speedSampleSize = 1000000000;
    private static final double significanceLevel = 0.05;

    @FunctionalInterface
    private interface Generator {
        double next();
    }

    private void testUniform(Generator g) {
        double[] cdf = new double[cdfSampleSize];
        for (int i = 0; i < cdfSampleSize; i++) {
            cdf[i] = g.next();
        }
        KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
        double p = ksTest.kolmogorovSmirnovTest(new UniformRealDistribution(0.0, 1.0), cdf);
        assertTrue(p >= significanceLevel, "Reject Null Hypothesis: The distribution is not uniform, p = " + p);
    }

    private void testGaussian(Generator g) {
        double[] cdf = new double[cdfSampleSize];
        for (int i = 0; i < cdfSampleSize; i++) {
            cdf[i] = g.next();
        }
        KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
        double p = ksTest.kolmogorovSmirnovTest(new NormalDistribution(0.0, 1.0), cdf);
        assertTrue(p >= significanceLevel, "Reject Null Hypothesis: The distribution is not uniform, p = " + p);
    }

    @Test
    public void testJavaUniform() {
        Random random = new Random();
        testUniform(random::nextDouble);
    }

    @Test
    public void testJavaGaussian() {
        Random random = new Random();
        testGaussian(random::nextGaussian);
    }

    @Test
    public void testJavaSpeed() {
        Random random = new Random();
        long start = System.currentTimeMillis();
        double hash = 0;
        for (int i = 0; i < speedSampleSize; i++) {
            hash = hash * random.nextDouble() + random.nextDouble();
        }
        long end = System.currentTimeMillis();
        System.out.println(hash);
        System.out.println("Java Uniform Time used: " + (end - start) + "ms");
    }

    @Test
    public void testJavaGaussianSpeed() {
        Random random = new Random();
        long start = System.currentTimeMillis();
        double hash = 0;
        for (int i = 0; i < speedSampleSize; i++) {
            hash = hash * random.nextGaussian() + random.nextGaussian();
        }
        long end = System.currentTimeMillis();
        System.out.println(hash);
        System.out.println("Java Gaussian Time used: " + (end - start) + "ms");
    }

    @Test
    public void testXorShift128() {
        XorShift128 xorShift = new XorShift128();
        testUniform(xorShift::nextDouble);
    }

    @Test
    public void testXorShift128Gaussian() {
        XorShift128 xorShift = new XorShift128();
        testGaussian(xorShift::nextGaussian);
    }

    @Test
    public void testXorShift128Speed() {
        XorShift128 xorShift = new XorShift128();
        long start = System.currentTimeMillis();
        double hash = 0;
        for (int i = 0; i < speedSampleSize; i++) {
            hash = hash * xorShift.nextDouble() + xorShift.nextDouble();
        }
        long end = System.currentTimeMillis();
        System.out.println(hash);
        System.out.println("XorShift Uniform Time used: " + (end - start) + "ms");
    }

    @Test
    public void testXorShift128GaussianSpeed() {
        XorShift128 xorShift = new XorShift128();
        long start = System.currentTimeMillis();
        double hash = 0;
        for (int i = 0; i < speedSampleSize; i++) {
            hash = hash * xorShift.nextGaussian() + xorShift.nextGaussian();
        }
        long end = System.currentTimeMillis();
        System.out.println(hash);
        System.out.println("XorShift Gaussian Time used: " + (end - start) + "ms");
    }
}
