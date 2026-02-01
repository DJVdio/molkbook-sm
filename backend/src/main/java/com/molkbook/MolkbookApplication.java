package com.molkbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MolkbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(MolkbookApplication.class, args);
    }
}
