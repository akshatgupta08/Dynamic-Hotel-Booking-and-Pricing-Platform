package com.example.demo.dto;


import com.example.demo.entity.User;
import com.example.demo.entity.enums.Gender;
import lombok.Data;

//We require the guest id here, because we will pass in a list of guest id's while making a booking.
@Data
public class GuestDto {
    private Long id;
    private String name;
    private Gender gender;
    private Integer age;
}