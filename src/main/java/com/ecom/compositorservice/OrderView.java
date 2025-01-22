package com.ecom.compositorservice;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderView
{
    String orderId;
    String customerId;
    String productId;
    Integer quantity;
    String orderStatus;
}
