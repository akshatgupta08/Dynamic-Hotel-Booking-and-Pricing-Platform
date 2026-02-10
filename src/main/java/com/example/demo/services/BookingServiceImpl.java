package com.example.demo.services;

import com.example.demo.dto.BookingDto;
import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.GuestDto;
import com.example.demo.dto.HotelReportDto;
import com.example.demo.entity.*;
import com.example.demo.entity.enums.BookingStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.UnAuthorisedException;
import com.example.demo.repositories.*;
import com.example.demo.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.demo.util.AppUtils.getCurrentUser;

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
    private final CheckoutService checkoutService;
    private final PricingService pricingService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    //after 10 minutes, when the booking fails automatically, we take the rooms back to the available pool.
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {//reserve the rooms for 10 minutes so that the
        //person booking gets time to book.

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

        //here we check if we have the inventory for the required number of days.
        if (inventoryList.size() != daysCount) {
            throw new IllegalStateException("Room is not available anymore");
        }

        // Reserve the rooms and update the inventory.
        inventoryRepository.initBooking(room.getId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());


        // Create the Booking

        //calculating the dynamic price of the booking.

        //The dynamic price of rooms is calculated for each day and then summed.

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .build();

        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    //The user trying to add guests should be the one who owns the booking.
    //before adding new guests, we must check if the booking has expired or not.
    //You get the time of creation of the booking. add 10 minutes to it and see if the
    // current time is lesser than the expiry time.
    //DO we remove the bookings that have expired.
    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<Long> guestIdList) {

        log.info("Adding guests for booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));
        User user = getCurrentUser();

        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has already expired");
        }

        if(booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is not under reserved state, cannot add guests");
        }

        for (Long guestId: guestIdList) {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: "+guestId));
            booking.getGuests().add(guest);
        }

        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }


    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    @Override
    public List<BookingDto> getMyBookings() {
        User user = getCurrentUser();

        return bookingRepository.findByUser(user)
                .stream().
                map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        //The booking has been locked to avoid the case where it is detected that the booking has not expired
        //and then the cron job expires the booking and frees up inventory. (Concurrency Control)

        bookingRepository.lockBooking(booking.getId());

        if (hasBookingExpired(booking)) {

            throw new IllegalStateException("Booking has already expired");
        }

        //we get the sessionUrl and that is what the front end redirects the user to.
        // Now the stripe server handle the check out logic
        //upon successful checkout, Strip redirects user to the success url
        // otherwise the user is redirected to the failure url.
        String sessionUrl = checkoutService.getCheckoutSession(booking,
                frontendUrl+"/payments/" +bookingId +"/status",
                frontendUrl+"/payments/" +bookingId +"/status");

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);

        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) return;

            String sessionId = session.getId();
            Booking booking =
                    bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
                            new ResourceNotFoundException("Booking not found for session ID: "+sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            //Concurrency control
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
        } else {
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    //if the payment has been made, the booking can be cancelled and the user gets refund.
    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        if(booking.getBookingStatus() != BookingStatus.CONFIRMED &&
                booking.getBookingStatus() != BookingStatus.PAYMENTS_PENDING) {
            throw new IllegalStateException("Only confirmed bookings or bookings whose payment is pending can be cancelled");
        }

        /*inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());*/

        if(booking.getBookingStatus() == BookingStatus.CONFIRMED) {

            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            inventoryRepository.findAndLockBookedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            // handle the refund
            try {
                Session session = Session.retrieve(booking.getPaymentSessionId());
                RefundCreateParams refundParams = RefundCreateParams.builder()
                        .setPaymentIntent(session.getPaymentIntent())
                        .build();

                Refund.create(refundParams);
            } catch (StripeException e) {
                throw new RuntimeException(e);
            }

        } else {

            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            inventoryRepository.cancelPaymentPendingBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
        }
    }


@Override
public BookingStatus getBookingStatus(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId).orElseThrow(
            () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
    );
    User user = getCurrentUser();
    if (!user.equals(booking.getUser())) {
        throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
    }

    if(hasBookingExpired(booking)) {
        return BookingStatus.EXPIRED;
    }

    return booking.getBookingStatus();
}

@Override
public List<BookingDto> getAllBookingsByHotelId(Long hotelId) {
    Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
            "found with ID: "+hotelId));
    User user = getCurrentUser();

    log.info("Getting all booking for the hotel with ID: {}", hotelId);

    if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);

    List<Booking> bookings = bookingRepository.findByHotel(hotel);

    return bookings.stream()
            .map((element) -> modelMapper.map(element, BookingDto.class))
            .collect(Collectors.toList());
}

//checks whether the user owns the hotel and then allows the user to get the revenue.
@Override
public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {

    Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
            "found with ID: "+hotelId));
    User user = getCurrentUser();

    log.info("Generating report for hotel with ID: {}", hotelId);

    if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);

    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

    List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);

    Long totalConfirmedBookings = bookings
            .stream()
            .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
            .count();

    BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
            .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
            .map(Booking::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal avgRevenue = totalConfirmedBookings == 0 ? BigDecimal.ZERO :
            totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP);

    return new HotelReportDto(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);
}



}

//talk about the reserved count feature and how it is different from the booked count and why it helps.
