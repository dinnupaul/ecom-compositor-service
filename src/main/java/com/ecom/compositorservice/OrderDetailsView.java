package com.ecom.compositorservice;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailsView
{
    private OrderView orderView;
    private ProductView productView;
    private PaymentView paymentView;
    private boolean complete;
    private String errorMessage;


    public OrderDetailsView(OrderView orderView, ProductView productView, PaymentView paymentView) {
        this.orderView = orderView;
        this.productView = productView;
        this.paymentView = paymentView;
    }
}
