package com.conceptarena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.conceptarena")
@EntityScan(basePackages = "com.conceptarena.infra.persistence.jpa")
@EnableJpaRepositories(basePackages = "com.conceptarena.infra.persistence.jpa")
public class ConceptArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConceptArenaApplication.class, args);
    }
}
