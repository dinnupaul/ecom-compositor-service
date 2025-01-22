package com.ecom.compositorservice;

import lombok.*;

import java.sql.Timestamp;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentView
{
    String paymentId;
    String customerId;
    String orderId;
    Integer amount;
    String paymentStatus;
    String paymentMethod;
    Timestamp createdAt;
}
