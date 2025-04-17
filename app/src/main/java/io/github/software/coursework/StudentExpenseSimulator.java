// 文件：StudentExpenseSimulator.java
package io.github.software.coursework;

import java.time.LocalDate;
import java.util.*;

// 枚举类：消费类别
enum Category {
    HOUSING, CLOTHING, COMMUNICATION, TRANSPORTATION, ELECTRONICS,
    NECESSITIES, FOOD, HOBBY, STUDY, MEDICAL, ENTERTAINMENT
}

// 实体类：消费记录
class Transaction {
    private LocalDate date;
    private double amount;  // 美元金额
    private Category category;
    private String description;

    // 汇率：美元 -> 人民币
    private static final double USD_TO_CNY = 7.0;

    public Transaction(LocalDate date, double amount, Category category, String description) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public String toString() {
        double amountInCNY = amount * USD_TO_CNY; // 将金额从美元换算成人民币
        return date + " | " + category + " | ¥" + String.format("%.2f", amountInCNY) + " | " + description;
    }
}

// 接口：分布模型
interface DistributionModel {
    double generateAmount();
}

// 正态分布
class NormalModel implements DistributionModel {
    private double mean;
    private double stddev;
    private Random random = new Random();

    public NormalModel(double mean, double stddev) {
        this.mean = mean;
        this.stddev = stddev;
    }

    public double generateAmount() {
        return Math.max(0, mean + stddev * random.nextGaussian());
    }
}

// 对数正态分布
class LogNormalModel implements DistributionModel {
    private double mu;
    private double sigma;
    private Random random = new Random();

    public LogNormalModel(double mu, double sigma) {
        this.mu = mu;
        this.sigma = sigma;
    }

    public double generateAmount() {
        return Math.max(0, Math.exp(mu + sigma * random.nextGaussian()));
    }
}

// 零膨胀帕累托分布
class ZeroInflatedParetoModel implements DistributionModel {
    private double shape;
    private double scale;
    private double zeroProbability;
    private Random random = new Random();

    public ZeroInflatedParetoModel(double shape, double scale, double zeroProbability) {
        this.shape = shape;
        this.scale = scale;
        this.zeroProbability = zeroProbability;
    }

    public double generateAmount() {
        if (random.nextDouble() < zeroProbability) return 0;
        return scale / Math.pow(random.nextDouble(), 1.0 / shape);
    }
}

// 混合模型：正态 + 零膨胀帕累托
class MixedHousingModel implements DistributionModel {
    private NormalModel normalPart;
    private ZeroInflatedParetoModel paretoPart;
    private Random random = new Random();
    private double normalWeight; // 占比

    public MixedHousingModel(NormalModel normalPart, ZeroInflatedParetoModel paretoPart, double normalWeight) {
        this.normalPart = normalPart;
        this.paretoPart = paretoPart;
        this.normalWeight = normalWeight;
    }

    public double generateAmount() {
        if (random.nextDouble() < normalWeight) {
            return normalPart.generateAmount();
        } else {
            return paretoPart.generateAmount();
        }
    }
}

// 类别模型映射
class CategoryModelMapper {
    private Map<Category, DistributionModel> modelMap = new HashMap<>();

    public CategoryModelMapper() {
        modelMap.put(Category.FOOD, new NormalModel(25, 5));
        modelMap.put(Category.CLOTHING, new LogNormalModel(3.0, 0.7));
        modelMap.put(Category.COMMUNICATION, new NormalModel(60, 10));
        modelMap.put(Category.TRANSPORTATION, new NormalModel(80, 20));
        modelMap.put(Category.ELECTRONICS, new ZeroInflatedParetoModel(2.5, 300, 0.85));
        modelMap.put(Category.NECESSITIES, new NormalModel(100, 20));
        modelMap.put(Category.HOBBY, new LogNormalModel(3.5, 0.8));
        modelMap.put(Category.STUDY, new ZeroInflatedParetoModel(3, 100, 0.7));
        modelMap.put(Category.MEDICAL, new ZeroInflatedParetoModel(2.8, 200, 0.9));
        modelMap.put(Category.ENTERTAINMENT, new LogNormalModel(3.2, 0.6));

        // 混合模型：住房 = 正态（房租）+ 帕累托（旅游/酒店）
        NormalModel rent = new NormalModel(1000, 100);
        ZeroInflatedParetoModel travel = new ZeroInflatedParetoModel(2.5, 400, 0.85);
        modelMap.put(Category.HOUSING, new MixedHousingModel(rent, travel, 0.8));
    }

    public DistributionModel getModel(Category category) {
        return modelMap.get(category);
    }
}

// 消费记录生成器
class TransactionGenerator {
    private CategoryModelMapper mapper;
    private Random random = new Random();

    public TransactionGenerator(CategoryModelMapper mapper) {
        this.mapper = mapper;
    }

    public Transaction generateTransaction(Category category, LocalDate date) {
        double amount = mapper.getModel(category).generateAmount();
        String description = category.toString().toLowerCase() + " spending";
        return new Transaction(date, amount, category, description);
    }

    public List<Transaction> generateTransactions(int days) {
        List<Transaction> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            for (Category cat : Category.values()) {
                if (random.nextDouble() < 0.7) { // 每类每天有一定概率消费
                    result.add(generateTransaction(cat, date));
                }
            }
        }
        return result;
    }
}

// 主程序
public class StudentExpenseSimulator {
    public static void main(String[] args) {
        CategoryModelMapper mapper = new CategoryModelMapper();
        TransactionGenerator generator = new TransactionGenerator(mapper);
        List<Transaction> transactions = generator.generateTransactions(30); // 生成 30 天账单

        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }
}
