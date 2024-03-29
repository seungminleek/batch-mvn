package com.example.batchmvn;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableBatchProcessing
@SpringBootApplication
public class BatchMvnApplication {


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public static void main(String[] args) {
        SpringApplication.run(BatchMvnApplication.class, args);
    }

}
