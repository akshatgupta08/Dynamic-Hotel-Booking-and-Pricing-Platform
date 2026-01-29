package com.example.demo.strategy;

import com.example.demo.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal calculatePrice(Inventory inventory);

}
