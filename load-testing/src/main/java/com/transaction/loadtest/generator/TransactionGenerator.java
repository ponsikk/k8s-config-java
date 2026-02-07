package com.transaction.loadtest.generator;

import com.transaction.models.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TransactionGenerator {

    private static final List<String> MERCHANTS = List.of(
            "Amazon", "Walmart", "Target", "Best Buy", "Home Depot",
            "Costco", "Starbucks", "McDonald's", "Uber", "Airbnb",
            "Netflix", "Apple Store", "Google Play", "Steam", "PlayStation Store"
    );

    private static final List<String> MERCHANT_CATEGORIES = List.of(
            "RETAIL", "FOOD", "TRAVEL", "ENTERTAINMENT", "UTILITIES",
            "HEALTHCARE", "EDUCATION", "AUTOMOTIVE", "SERVICES"
    );

    private static final List<String> CURRENCIES = List.of("USD", "EUR", "RUB");

    private static final List<Transaction.Location> LOCATIONS = List.of(
            Transaction.Location.builder().country("USA").city("New York").build(),
            Transaction.Location.builder().country("USA").city("Los Angeles").build(),
            Transaction.Location.builder().country("USA").city("Chicago").build(),
            Transaction.Location.builder().country("UK").city("London").build(),
            Transaction.Location.builder().country("France").city("Paris").build(),
            Transaction.Location.builder().country("Germany").city("Berlin").build(),
            Transaction.Location.builder().country("Russia").city("Moscow").build(),
            Transaction.Location.builder().country("Russia").city("Saint Petersburg").build(),
            Transaction.Location.builder().country("Japan").city("Tokyo").build(),
            Transaction.Location.builder().country("China").city("Beijing").build(),
            Transaction.Location.builder().country("India").city("Mumbai").build(),
            Transaction.Location.builder().country("Brazil").city("Sao Paulo").build()
    );

    private static final Transaction.TransactionType[] TRANSACTION_TYPES =
            Transaction.TransactionType.values();

    public Transaction generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Generate random userId from user_1 to user_10000
        String userId = "user_" + random.nextInt(1, 10001);

        // Generate random amount between 1.00 and 5000.00
        double amountValue = random.nextDouble(1.00, 5000.00);
        BigDecimal amount = BigDecimal.valueOf(amountValue).setScale(2, RoundingMode.HALF_UP);

        // Random currency
        String currency = CURRENCIES.get(random.nextInt(CURRENCIES.size()));

        // Random merchant
        String merchant = MERCHANTS.get(random.nextInt(MERCHANTS.size()));

        // Random merchant category
        String merchantCategory = MERCHANT_CATEGORIES.get(random.nextInt(MERCHANT_CATEGORIES.size()));

        // Random transaction type
        Transaction.TransactionType type = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];

        // Random location
        Transaction.Location location = LOCATIONS.get(random.nextInt(LOCATIONS.size()));

        return Transaction.builder()
                .transactionId(UUID.randomUUID())
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .merchant(merchant)
                .merchantCategory(merchantCategory)
                .type(type)
                .location(location)
                .timestamp(Instant.now())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }
}
