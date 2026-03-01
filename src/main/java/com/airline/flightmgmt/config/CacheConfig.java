package com.airline.flightmgmt.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        //We can optimized to load early and load only next 2 month data
        //because next 2 months flight are more quried and booked
        //Daily flight can vary between 2000 to 5000
        //So 15000 flight max

        //Every object of flight take approx 1KG 2 GB of data, that is okay with distributed cache like redis
        cacheManager.registerCustomCache("flight", Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(150000)
                .build());

        cacheManager.registerCustomCache("bookingDates", Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(20000)
                .build());

        return cacheManager;
    }
}