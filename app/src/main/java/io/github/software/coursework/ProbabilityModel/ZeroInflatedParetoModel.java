package io.github.software.coursework.ProbabilityModel;

import java.util.Random;

public class ZeroInflatedParetoModel implements DistributionModel {
    private final double shape;
    private final double scale;
    private final double zeroProbability;

    public ZeroInflatedParetoModel(double shape, double scale, double zeroProbability) {
        this.shape = shape;
        this.scale = scale;
        this.zeroProbability = zeroProbability;
    }

    @Override
    public double generateAmount(Random random) {
        if (random.nextDouble() < zeroProbability) return 0;
        return scale / Math.pow(random.nextDouble(), 1.0 / shape);
    }
}
