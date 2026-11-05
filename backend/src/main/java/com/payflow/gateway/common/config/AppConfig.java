package com.payflow.gateway.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Configuration
public class AppConfig {

    @Bean
    public Set<String> supportedCurrencies() {
        return new HashSet<>(Arrays.asList("INR", "USD", "EUR", "GBP"));
    }
}

