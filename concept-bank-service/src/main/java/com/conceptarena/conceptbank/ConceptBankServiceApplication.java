package com.conceptarena.conceptbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConceptBankServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConceptBankServiceApplication.class, args);
    }
}
