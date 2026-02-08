package com.example.demo.services;

import com.example.demo.dto.HotelDto;
import com.example.demo.dto.HotelInfoDto;
import com.example.demo.dto.HotelInfoRequestDto;

import java.util.List;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

    void deleteHotelById(Long id);

    void activateHotel(Long hotelId);

    HotelDto updateHotelById(Long id, HotelDto hotelDto);

    HotelInfoDto getHotelInfoById(Long hotelId, HotelInfoRequestDto hotelInfoRequestDto);

    List<HotelDto> getAllHotels();
}
