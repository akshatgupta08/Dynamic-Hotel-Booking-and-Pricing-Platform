package com.example.demo.dto;

import com.example.demo.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserDto {

    private Long id;
    private String email;
    private String name;
    private Gender gender;
    private LocalDate dateOfBirth;

}

//I could add validations here to ensure that the email is given in the right format.
