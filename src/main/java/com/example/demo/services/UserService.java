package com.example.demo.services;

import com.example.demo.dto.ProfileUpdateRequestDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.Role;

public interface UserService {

    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

    UserDto getMyProfile();

    void addRole(Long id, Role role);

    void makeManager(Long id);
}
