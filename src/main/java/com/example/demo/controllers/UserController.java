package com.example.demo.controllers;

import com.example.demo.dto.BookingDto;
import com.example.demo.dto.GuestDto;
import com.example.demo.dto.ProfileUpdateRequestDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.BookingService;
import com.example.demo.services.GuestService;
import com.example.demo.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    @Value("${adminKey}")
    private String adminKey;

    private final UserService userService;
    private final BookingService bookingService;
    private final GuestService guestService;
    private final UserRepository userRepository;

    @PatchMapping("/profile")
    @Operation(summary = "Update the user profile", tags = {"Profile"})
    public ResponseEntity<Void> updateProfile(@RequestBody ProfileUpdateRequestDto profileUpdateRequestDto) {
        userService.updateProfile(profileUpdateRequestDto);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myBookings")
    @Operation(summary = "Get all my previous bookings", tags = {"Profile"})
    public ResponseEntity<List<BookingDto>> getMyBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings());
    }

    @GetMapping("/profile")
    @Operation(summary = "Get my Profile", tags = {"Profile"})
    public ResponseEntity<UserDto> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @GetMapping("/guests")
    @Operation(summary = "Get all my guests", tags = {"Booking Guests"})
    public ResponseEntity<List<GuestDto>> getAllGuests() {
        return ResponseEntity.ok(guestService.getAllGuests());
    }

    @PostMapping("/guests")
    @Operation(summary = "Add a new guest to my guests list", tags = {"Booking Guests"})
    public ResponseEntity<GuestDto> addNewGuest(@RequestBody GuestDto guestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.addNewGuest(guestDto));
    }

    @PutMapping("guests/{guestId}")
    @Operation(summary = "Update a guest", tags = {"Booking Guests"})
    public ResponseEntity<Void> updateGuest(@PathVariable Long guestId, @RequestBody GuestDto guestDto) {
        guestService.updateGuest(guestId, guestDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("guests/{guestId}")
    @Operation(summary = "Remove a guest", tags = {"Booking Guests"})
    public ResponseEntity<Void> deleteGuest(@PathVariable Long guestId) {
        guestService.deleteGuest(guestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("makeAdmin/{key}")
    public ResponseEntity<Void> makeAdmin(@PathVariable String key) {

        if(key.equals(adminKey) && !userRepository.existsByRolesContaining(Role.ADMIN)) {
           UserDto admin = userService.getMyProfile();
           userService.addRole(admin.getId(), Role.ADMIN);
        } else {
            throw new AccessDeniedException("Cannot be the admin");
        }

        return ResponseEntity.noContent().build();
    }
}

// See how would users be authenticated to access these api's.
