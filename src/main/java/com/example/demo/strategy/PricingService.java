package com.example.demo.strategy;

import com.example.demo.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {
    //takes in a single inventory, passes it through all the strategies and gets the total price of that single inventory.
    //We get the price of the single room present in that inventory row.
    public BigDecimal calculateDynamicPricing(Inventory inventory) {
        PricingStrategy pricingStrategy = new BasePricingStrategy();

        // apply the additional strategies
        pricingStrategy = new SurgePricingStrategy(pricingStrategy);
        pricingStrategy = new OccupancyPricingStrategy(pricingStrategy);
        pricingStrategy = new UrgencyPricingStrategy(pricingStrategy);
        pricingStrategy = new HolidayPricingStrategy(pricingStrategy);

        return pricingStrategy.calculatePrice(inventory);

        //we are going through all the strategies and increasing the price according to the valid startegies.
        // this is the decorator design pattern.
    }

    public BigDecimal calculateTotalPrice(List<Inventory> inventoryList) {
        //think of multiple inventory rows as multiple inventory days.
        return inventoryList.stream()
                .map(this::calculateDynamicPricing)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// WHy dd we not put all the code in a single file. It was very doable?
//Separation of Concerns, can add more strategies without modifying the exisitng files.