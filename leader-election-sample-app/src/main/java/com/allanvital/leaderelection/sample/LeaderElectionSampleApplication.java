package com.allanvital.leaderelection.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Allan Vital (https://allanvital.com)
 */
@SpringBootApplication
@EnableScheduling
public class LeaderElectionSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaderElectionSampleApplication.class, args);
    }
}
