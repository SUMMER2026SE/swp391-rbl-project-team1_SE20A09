package com.sportvenue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportVenueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportVenueApplication.class, args);
    }
}
