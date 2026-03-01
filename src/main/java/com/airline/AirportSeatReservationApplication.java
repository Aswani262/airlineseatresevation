package com.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableAspectJAutoProxy
@EnableCaching
public class AirportSeatReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirportSeatReservationApplication.class, args);
	}

}
