package com.example.demo.services;

import com.example.demo.dto.HotelDto;
import com.example.demo.dto.HotelSearchRequest;
import com.example.demo.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelDto> searchHotels(HotelSearchRequest hotelSearchRequest);

}
