package com.example.demo.entity.enums;


public enum BookingStatus {
    RESERVED,  // we just reserved some rooms in a hotel, no guests have been added yet.
    GUESTS_ADDED,  // The Guests have been added but the payment has not been made.
    CONFIRMED,  // The payment has been made.
    CANCELLED,  // The booking got cancelled.\
    EXPIRED
}