package io.github.software.coursework.util;

import java.security.SecureRandom;

public final class XorShift128 {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long floatMask = (1L << 52) - 1;
    private static final double doubleEps = 1.0 / (1L << 52);
    private long state0, state1;
    private double nextGaussian;
    private boolean hasNextGaussian = false;

    public XorShift128(long state0, long state1) {
        this.state0 = state0;
        this.state1 = state1;
    }

    public XorShift128() {
        this(RANDOM.nextLong(), RANDOM.nextLong());
    }

    public long nextLong() {
        long s1 = state0;
        long s0 = state1;
        state0 = s0;
        s1 ^= s1 << 23;
        s1 ^= s1 >>> 18;
        s1 ^= s0;
        s1 ^= s0 >>> 5;
        state1 = s1;
        return s1 + s0;
    }

    public double nextDouble() {
        return (nextLong() & floatMask) * doubleEps;
    }

    public int nextInt() {
        return (int) nextLong();
    }

    public int nextInt(int bound) {
        if ((bound & (bound - 1)) == 0) {
            return (int) (nextLong() & (bound - 1));
        } else {
            long rep = Long.MAX_VALUE - Long.MAX_VALUE % bound;
            long next = nextLong() & Long.MAX_VALUE;
            while (next >= rep) {
                next = nextLong() & Long.MAX_VALUE;
            }
            return (int) (next % bound);
        }
    }

    public double nextGaussian() {
        if (hasNextGaussian) {
            hasNextGaussian = false;
            return nextGaussian;
        }
        double v1, v2, s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = Math.sqrt(-2 * Math.log(s) / s);
        nextGaussian = v2 * multiplier;
        hasNextGaussian = true;
        return v1 * multiplier;
    }
}
