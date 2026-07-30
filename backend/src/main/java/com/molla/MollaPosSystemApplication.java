package com.molla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MollaPosSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(MollaPosSystemApplication.class, args);
	}

}
