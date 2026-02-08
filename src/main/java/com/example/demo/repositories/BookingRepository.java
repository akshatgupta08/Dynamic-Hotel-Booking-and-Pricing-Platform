package com.example.demo.repositories;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel hotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Booking> findByUser(User user);

    @Query("""
    SELECT b
    FROM Booking b
    WHERE b.createdAt < :expiryTime
      AND b.bookingStatus = :status
""")
    List<Booking> findExpiredBookings(
            @Param("expiryTime") LocalDateTime expiryTime,
            @Param("status") BookingStatus status
    );



}
