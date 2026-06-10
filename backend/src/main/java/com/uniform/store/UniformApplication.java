package com.uniform.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UniformApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniformApplication.class, args);
    }
}
