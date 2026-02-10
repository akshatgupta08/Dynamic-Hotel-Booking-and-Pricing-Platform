package com.example.demo.controllers;

import com.example.demo.dto.RoomDto;
import com.example.demo.services.RoomService;
import com.example.demo.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/makeManager")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PutMapping("/{userId}")
    public ResponseEntity<Void> addManager(@PathVariable Long userId) {

        userService.makeManager(userId);

        return ResponseEntity.noContent().build();
    }
}



