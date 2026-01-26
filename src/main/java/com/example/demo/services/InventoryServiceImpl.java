package com.example.demo.services;


import com.example.demo.dto.HotelDto;
import com.example.demo.dto.HotelSearchRequest;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.Inventory;
import com.example.demo.entity.Room;
import com.example.demo.repositories.InventoryRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService{

    private final InventoryRepository inventoryRepository;

    private final ModelMapper modelMapper;

    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for (;!today.isAfter(endDate); today=today.plusDays(1)) {
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }
    }

    @Override
    public void deleteAllInventories(Room room) {
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        log.info("Searching hotels for {} city, from {} to {}", hotelSearchRequest.getCity(), hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());
        //getting the parameters of the pageable object.
        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());

        //counting the number of days we require the hotel for.
        long dateCount =
                ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate()) + 1;


        Page<Hotel> hotelPage = inventoryRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate(), hotelSearchRequest.getRoomsCount(),
                dateCount, pageable);

        //you return the Page holding hotelDto.
        return hotelPage.map((element) -> modelMapper.map(element, HotelDto.class));
    }
}

//It should not be the case that in a Hotel suppose you have 2 room types/
//One room type is available for certain period and the other room type is available for some other period.
// We want to check if any one room type is available for all the required dates.