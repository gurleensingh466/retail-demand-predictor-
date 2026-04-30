package com.smartretail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartRetailDemandApplication {
  public static void main(String[] args) {
    SpringApplication.run(SmartRetailDemandApplication.class, args);
  }
}

