package com.manoel.carteiradigital.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    Retryer feignRetryer() {
        // até 3 tentativas, com backoff de 200ms a 1s, para lidar com instabilidades transitórias da Pluggy
        return new Retryer.Default(200, 1000, 3);
    }
}
