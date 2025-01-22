package com.ecom.compositorservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductView {

    String productId;
    String productName;
    String productDesc;
    Integer price;
}
