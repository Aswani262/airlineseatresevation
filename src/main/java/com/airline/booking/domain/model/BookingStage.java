package com.airline.booking.domain.model;
//Failed or cancellation reason
public enum BookingStage {
    USER_CANCELLED,//Booking Cancelled
    PAYMENT_EXPIRED,//Booking Failed
    SYSTEM_ERROR,//Failed
    PAYMENT_FAILED,//Booking Failed
    PAYMENT_SUCCESS,//Booking Success
    FLIGHT_CANCELLED,
    PAYMENT_INITIATED
    //Booking Cancelled
}
