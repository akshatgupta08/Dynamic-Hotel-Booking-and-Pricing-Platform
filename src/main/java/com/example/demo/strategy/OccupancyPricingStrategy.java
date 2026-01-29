package com.example.demo.strategy;

import com.example.demo.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class OccupancyPricingStrategy implements PricingStrategy{

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        double occupancyRate = (double) inventory.getBookedCount() / inventory.getTotalCount();
        if(occupancyRate > 0.8) { //if occupancy is more than 80 percent.
            price = price.multiply(BigDecimal.valueOf(1.2));
        }
        return price;
    }
}
