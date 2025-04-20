package io.github.software.coursework.ProbabilityModel;

import java.time.LocalDate;

public class Transaction {
    private final LocalDate date;
    private final double amount;
    private final Category category;
    private final String description;

    public Transaction(LocalDate date, double amount, Category category, String description) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    @Override
    public String toString() {
        return date + " | " + category + " | ¥" + String.format("%.2f", amount) + " | " + description;
    }

    public LocalDate getDate() { return date; }
    public double getAmount() { return amount; }
    public Category getCategory() { return category; }
    public String getDescription() { return description; }
}
