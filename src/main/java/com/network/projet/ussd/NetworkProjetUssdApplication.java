package com.network.projet.ussd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Class
 * USSD Gateway Application with reactive programming
 * 
 * @author Network Project Team
 * @date 2026-01-17
 */
@SpringBootApplication
@EnableScheduling
public class NetworkProjetUssdApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetworkProjetUssdApplication.class, args);
    }
}