package com.example.demo.services;

import com.example.demo.dto.HotelDto;
import com.example.demo.dto.HotelInfoDto;
import com.example.demo.dto.RoomDto;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.Room;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.HotelRepository;
import com.example.demo.repositories.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
    @Slf4j
    @RequiredArgsConstructor
    public class HotelServiceImpl implements HotelService {

        private final HotelRepository hotelRepository;
        private final ModelMapper modelMapper;
        private final InventoryService inventoryService;
        private final RoomRepository roomRepository;

        @Override
        public HotelDto createNewHotel(HotelDto hotelDto) {
            log.info("Creating a new hotel with name: {}", hotelDto.getName());
            Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
            hotel.setActive(false); // At first there will be no inventory for this hotel, so it will not be shown as active.
            hotel = hotelRepository.save(hotel);
            log.info("Created a new hotel with ID: {}", hotelDto.getId());
            return modelMapper.map(hotel, HotelDto.class);
        }

        @Override
        public HotelDto getHotelById(Long id) {
            log.info("Getting the hotel with ID: {}", id);
            Hotel hotel = hotelRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
            return modelMapper.map(hotel, HotelDto.class);
        }

        @Override
        public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
            log.info("Updating the hotel with ID: {}", id);
            Hotel hotel = hotelRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));
            modelMapper.map(hotelDto, hotel);
            hotel.setId(id);
            hotel = hotelRepository.save(hotel);
            return modelMapper.map(hotel, HotelDto.class);
        }

        @Override
        @Transactional //important to put here because we want both the operations
        //in databases to be completed or none of them.
        public void deleteHotelById(Long id) {
            log.info("Deleting the hotel with id {}", id);
            Hotel hotel = hotelRepository
                    .findById(id)
                    .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with id: " + id));
            //Since the hotel is being deleted, all the rooms present in the inventory associated to this hotel
            // are to be removed starting from the current localDate.

            for (Room room : hotel.getRooms()) {
                inventoryService.deleteAllInventories(room); //this is giving an error when
                     //we try to delete the rooms that are not there yet.
                roomRepository.deleteById(room.getId());
            }

            hotelRepository.deleteById(id);
        }

        @Override
        @Transactional
        public void activateHotel(Long hotelId) {
            log.info("Activating the hotel with ID: {}", hotelId);
            Hotel hotel = hotelRepository
                    .findById(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

            hotel.setActive(true);

            // assuming that we only activate the hotel once
            // once the hotel is activated, we fill the inventory.
            for (Room room : hotel.getRooms()) {
                inventoryService.initializeRoomForAYear(room);
            }
        }


        @Override
        public HotelInfoDto getHotelInfoById(Long hotelId) {
            Hotel hotel = hotelRepository
                    .findById(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: "+ hotelId));

            List<RoomDto> rooms = hotel.getRooms()
                    .stream()
                    .map((element) -> modelMapper.map(element, RoomDto.class))
                    .toList();

            return new HotelInfoDto(modelMapper.map(hotel, HotelDto.class), rooms);
        }

    }