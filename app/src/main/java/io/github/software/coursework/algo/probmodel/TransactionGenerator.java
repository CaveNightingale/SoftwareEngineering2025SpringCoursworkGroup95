package io.github.software.coursework.algo.probmodel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TransactionGenerator {
    private final CategoryModelMapper mapper;
    private final Random random = new Random();

    public TransactionGenerator(CategoryModelMapper mapper) {
        this.mapper = mapper;
    }

    public Transaction generateTransaction(Category category, LocalDate date) {
        double amount = mapper.getModel(category).generateAmount(random);
        String description = category.toString().toLowerCase() + " spending";
        return new Transaction(date, amount, category, description);
    }

    public List<Transaction> generateTransactions() {
        List<Transaction> result = new ArrayList<>();
        LocalDate date = LocalDate.now();
        for (Category cat : Category.values()) {
            if (random.nextDouble() < 0.8) {
                result.add(generateTransaction(cat, date));
            }
        }
        return result;
    }
}
