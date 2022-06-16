package com.wh.business.collectiontask.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RibbonConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}