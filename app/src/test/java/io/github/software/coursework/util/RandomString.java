package io.github.software.coursework.util;

import java.util.Random;

public class RandomString {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ALPHABET_LENGTH = ALPHABET.length();

    public static String generateFixedLength(int length, Random random) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHABET.charAt(random.nextInt(ALPHABET_LENGTH)));
        }
        return stringBuilder.toString();
    }

    /**
     * Generate random string whose length follows geometric distribution.
     * @param expectation The expectation of the length.
     * @param random the random number generator.
     * @return the generated string.
     */
    public static String generateGeometricLength(double expectation, Random random) {
        StringBuilder stringBuilder = new StringBuilder();
        double endProbability = 1.0 / expectation;
        do {
            stringBuilder.append(ALPHABET.charAt(random.nextInt(ALPHABET_LENGTH)));
        } while (random.nextDouble() > endProbability);
        return stringBuilder.toString();
    }
}
