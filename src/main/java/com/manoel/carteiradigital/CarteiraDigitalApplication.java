package com.manoel.carteiradigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CarteiraDigitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarteiraDigitalApplication.class, args);
    }
}
