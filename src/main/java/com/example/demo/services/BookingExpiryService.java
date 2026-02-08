package com.example.demo.services;


import com.example.demo.entity.Booking;
import com.example.demo.entity.enums.BookingStatus;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Transactional
public class BookingExpiryService {

   private final BookingRepository bookingRepository;

   private final InventoryRepository inventoryRepository;

    @Scheduled(cron = "0 * * * * *")
    public List<Booking> expireBookings() {
      //get the expired bookings first
        //only set the reserved bookings to Expired state.
        List<Booking> expiredBookings =
                bookingRepository.findExpiredBookings(
                        LocalDateTime.now().minusMinutes(10),
                        BookingStatus.RESERVED
                );


        for(Booking booking : expiredBookings) {
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                                                            booking.getCheckOutDate(), booking.getRoomsCount());

            inventoryRepository.expireBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            booking.setBookingStatus(BookingStatus.EXPIRED);

        }

        return bookingRepository.saveAll(expiredBookings);
    }

}
