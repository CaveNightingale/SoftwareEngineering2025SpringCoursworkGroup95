package io.github.software.coursework.ProbabilityModel;

import java.util.HashMap;
import java.util.Map;

public class CategoryModelMapper {
    private final Map<Category, DistributionModel> modelMap = new HashMap<>();

    public CategoryModelMapper() {
        modelMap.put(Category.FOOD, new NormalModel(200, 50));
        modelMap.put(Category.CLOTHING, new LogNormalModel(2.5, 0.6));
        modelMap.put(Category.COMMUNICATION, new NormalModel(100, 20));
        modelMap.put(Category.TRANSPORTATION, new NormalModel(300, 100));
        modelMap.put(Category.ELECTRONICS, new ZeroInflatedParetoModel(2.5, 1500, 0.7));
        modelMap.put(Category.NECESSITIES, new NormalModel(150, 40));
        modelMap.put(Category.HOBBY, new LogNormalModel(3.0, 0.7));
        modelMap.put(Category.STUDY, new ZeroInflatedParetoModel(3, 500, 0.8));
        modelMap.put(Category.MEDICAL, new ZeroInflatedParetoModel(3, 1000, 0.9));
        modelMap.put(Category.ENTERTAINMENT, new LogNormalModel(3.2, 0.8));

        NormalModel rent = new NormalModel(3000, 500);
        ZeroInflatedParetoModel travel = new ZeroInflatedParetoModel(2.5, 4000, 0.75);
        modelMap.put(Category.HOUSING, new MixedHousingModel(rent, travel, 0.85));
    }

    public DistributionModel getModel(Category category) {
        return modelMap.get(category);
    }
}
