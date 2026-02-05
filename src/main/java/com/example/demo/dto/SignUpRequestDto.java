package com.example.demo.dto;

import lombok.Data;

@Data
public class SignUpRequestDto {

    private String email, password;

    private Long id;

}
