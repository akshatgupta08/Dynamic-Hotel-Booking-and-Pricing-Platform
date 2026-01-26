package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelSearchRequest {

    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer roomsCount;

    private Integer page = 0; // by default the page is zero
    private Integer size = 10; // the page size is 10 by default

}

//When we are searching the hotels for booking, we would need the dates, number of rooms and the location.