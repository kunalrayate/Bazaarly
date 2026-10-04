package com.bazaarly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BazaarlyApplication {
    public static void main(String[] args) { SpringApplication.run(BazaarlyApplication.class, args); }
}
