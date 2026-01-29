package com.example.demo.strategy;

import com.example.demo.entity.Inventory;

import java.math.BigDecimal;

public class BasePricingStrategy implements PricingStrategy{
    @Override
    public BigDecimal calculatePrice(Inventory inventory) { //notice that we do not need any other pricing startegy
         // to be multiplied with this base pricing strategy. That's why it is called the base pricing strategy.
        return inventory.getRoom().getBasePrice(); // to get the base price
         //you will have to see the room instance's price in the inventory.
    }
}
