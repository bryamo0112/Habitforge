package com.habitforge.habitforge_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;  // add this import

@SpringBootApplication
@EnableScheduling  // Enable scheduling here
public class HabitForgeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitForgeBackendApplication.class, args);
    }
}



