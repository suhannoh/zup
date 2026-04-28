package com.noh.zup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ZupApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZupApplication.class, args);
    }
}
