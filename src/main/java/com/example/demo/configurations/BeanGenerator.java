package com.example.demo.configurations;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanGenerator {

    @Bean
    public ModelMapper getModelMapper() {

        return new ModelMapper();

    }
}
