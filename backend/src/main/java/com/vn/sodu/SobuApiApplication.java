package com.vn.sodu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableCaching
public class SobuApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SobuApiApplication.class, args);
	}

}
