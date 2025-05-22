package io.github.software.coursework.ProbabilityModel;

import java.util.Random;

public class MixedHousingModel implements DistributionModel {
    private final NormalModel normalPart;
    private final ZeroInflatedParetoModel paretoPart;
    private final double normalWeight;

    public MixedHousingModel(NormalModel normalPart, ZeroInflatedParetoModel paretoPart, double normalWeight) {
        this.normalPart = normalPart;
        this.paretoPart = paretoPart;
        this.normalWeight = normalWeight;
    }

    @Override
    public double generateAmount(Random random) {
        if (random.nextDouble() < normalWeight) {
            return normalPart.generateAmount(random);
        } else {
            return paretoPart.generateAmount(random);
        }
    }
}
