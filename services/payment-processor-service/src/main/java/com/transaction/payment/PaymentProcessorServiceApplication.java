package com.transaction.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PaymentProcessorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessorServiceApplication.class, args);
    }
}
