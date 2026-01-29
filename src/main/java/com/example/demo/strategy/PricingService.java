package com.example.demo.strategy;

import com.example.demo.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

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
}

// WHy dd we not put all the code in a single file. It was very doable?
//Separation of Concerns, can add more strategies without modifying the exisitng files.