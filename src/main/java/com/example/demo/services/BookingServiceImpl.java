package com.example.demo.services;

import com.example.demo.dto.BookingDto;
import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.GuestDto;
import com.example.demo.entity.*;
import com.example.demo.entity.enums.BookingStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.UnAuthorisedException;
import com.example.demo.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    //after 10 minutes, when the booking fails automatically, we take the rooms back to the avilable pool.
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {//reserve the room for 10 minutes so that the
        //person booking gets time to book. Plus we will also hold the same price for those 10 minutes.

        log.info("Initialising booking for hotel : {}, room: {}, date {}-{}", bookingRequest.getHotelId(),
                bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId()).orElseThrow(() ->
                new ResourceNotFoundException("Hotel not found with id: "+bookingRequest.getHotelId()));

        Room room = roomRepository.findById(bookingRequest.getRoomId()).orElseThrow(() ->
                new ResourceNotFoundException("Room not found with id: "+bookingRequest.getRoomId()));

        //for a particular room id, check in date, check out date, and the number of rooms available between
        // these dates is checked and the room rows or the inventory list is returned.
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        // calculated the days we want to book for.
        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate())+1;

        if (inventoryList.size() != daysCount) {
            throw new IllegalStateException("Room is not available anymore");
        }

        // Reserve the room/ update the booked count of inventories
        //update the reserved count, since we are able to successfully reserve the rooms.
        for(Inventory inventory: inventoryList) {
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        // save the inventories with the updated information.
        //The current transaction can update the rows.
        inventoryRepository.saveAll(inventoryList);

        // Create the Booking



        // TODO: calculate dynamic amount

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser()) //Maybe it is the user who is booking right now?
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN) // will be calculating the amount. The Locking also helps us to keep the
                //prices stable while booking is being done.
                .build();

        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    //The user trying to add guests should be the one who owns the booking.
    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
        //A person who does not own the booking could also be making bookings for other guests.
        //or some person who is not even a guest could start booking.
        //To protect against this I compare the current user with the user who owns the booking.
        log.info("Adding guests for booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));

        User user = getCurrentUser();

        if(!(user.equals(booking.getUser()))) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has already expired");
        }

        //Booking has to be reserved first only then you can add the guests.
        if(booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is not under reserved state, cannot add guests");
        }

        for (GuestDto guestDto: guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(getCurrentUser());
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest); // the guest is added to the join table (Many to Many relationship)
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    //before adding new guests, we must check if the booking has expired or not.
    //You get the time of creation of the booking. add 10 minutes to it and see if the
    // current time is lesser than the expiry time.
    //DO we remove the bookings that have expired.
    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}

//talk about the reserved count feature and how it is different from the booked count and why it helps.
