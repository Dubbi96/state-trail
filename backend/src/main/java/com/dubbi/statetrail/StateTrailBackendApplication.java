package com.dubbi.statetrail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StateTrailBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(StateTrailBackendApplication.class, args);
    }
}


