package com.airline.payment.jobs;

//Check regularly for the pending payment status
//If case of system crash - like suppose we payment gateway send the success/failed message
//at that time our system in able to catch the event
//Once our system up this schedular will check the status from payment gateway a fire a event of
// success or failure
public class DefaultPaymentStatusPoller implements PaymentStatusPoller {
}
