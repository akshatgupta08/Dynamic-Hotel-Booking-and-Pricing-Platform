package com.example.demo.services;

import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.repositories.BookingRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService{

    private final BookingRepository bookingRepository;

    @Override
    public String getCheckoutSession(Booking booking, String successUrl, String failureUrl) {
        log.info("Creating session for booking with ID: {}", booking.getId());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();
            Customer customer = Customer.create(customerParams);
            //using this SessionCreate Params we add the details about the product, the amount to be paid.
            // We set the mode to Payment and demand that the user must fill in the address details.
            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED) //The customer will
                                               //have to feed in the address details for the payment to be more secure.
                    .setCustomer(customer.getId()) //customer is basically the current user from the security context.
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(failureUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")    //The money needs to be specified in the lowest possible denomination
                                                    .setUnitAmount(booking.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(booking.getHotel().getName() +" : "+ booking.getRoom().getType())
                                                                    .setDescription("Booking ID: "+booking.getId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            //This makes the http request to the stripe's servers
            // and the unique session id is retrived upon successfull checkout.
            // we are storing the session id with the booking entity.
            //sets the payment intent, but does not charge the card yet.
            Session session = Session.create(sessionParams);
            booking.setPaymentSessionId(session.getId());
            bookingRepository.save(booking);



            log.info("Session created successfully for booking with ID: {}", booking.getId());
            // we return the session url back to the client.
            //using this url, the client can move to the Stripe's checkout page.
            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }


    }
}