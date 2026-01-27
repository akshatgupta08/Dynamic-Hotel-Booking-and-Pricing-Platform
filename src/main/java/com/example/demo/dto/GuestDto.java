package com.example.demo.dto;


import com.example.demo.entity.User;
import com.example.demo.entity.enums.Gender;
import lombok.Data;

@Data
public class GuestDto {
    private Long id;
    private User user;
    private String name;
    private Gender gender;
    private Integer age;
}