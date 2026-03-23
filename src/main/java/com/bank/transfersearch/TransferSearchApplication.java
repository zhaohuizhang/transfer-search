package com.bank.transfersearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TransferSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferSearchApplication.class, args);
    }

}
