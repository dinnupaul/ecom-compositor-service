package com.ecom.compositorservice;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/compositor")
public class ApiCompositorController {

    private static final Logger log = LoggerFactory.getLogger(ApiCompositorController.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("order_service_get_orders")
    WebClient webClientOrderServiceGetOrders;

    @Autowired
    @Qualifier("payment_service_get_payments")
    WebClient webClientPaymentServiceGetPayments;

    @Autowired
    @Qualifier("product_catalog_service_get_products")
    WebClient webClientProductServiceGetProducts;


    @Autowired
    @Qualifier("order_service_get_all")
    WebClient webClientOrderServiceGetAll;

    @Autowired
    @Qualifier("payment_service_get_all")
    WebClient webClientPaymentServiceGetAll;

    @Autowired
    @Qualifier("product_catalog_service_get_all")
    WebClient webClientProductServiceGetAll;


    @GetMapping("/allOrders")
    public Mono<ResponseEntity<?>> getAllOrderDetails(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        // COOKIE VALIDATION LOGIC
        List<Cookie> cookieList = null;

        //Optional<String> healthStatusCookie = Optional.ofNullable(request.getHeader("health_status_cookie"));
        Cookie[] cookies = httpServletRequest.getCookies();
        if(cookies == null)
        {
            cookieList = new ArrayList<>();
        }
        else
        {
            // REFACTOR TO TAKE NULL VALUES INTO ACCOUNT
            cookieList = List.of(cookies);
        }

        if( cookieList.stream().filter(cookie -> cookie.getName().equals("compositor-service-getorderdeatils-1")).findAny().isEmpty()) // COOKIE_CHECK
        {
            // FRESH REQUEST PROCESSING

            // Fetch all orders
            Flux<OrderView> orderFlux = webClientOrderServiceGetOrders.get()
                    .retrieve()
                    .bodyToFlux(OrderView.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> {
                        System.err.println("Error fetching orders: " + e.getMessage());
                        return Flux.empty(); // Return empty if service fails
                    });

            // Fetch all products
            Mono<Map<String, ProductView>> productMapMono = webClientProductServiceGetProducts.get()
                    .retrieve()
                    .bodyToFlux(ProductView.class)
                    .collectMap(ProductView::getProductId)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> {
                        System.err.println("Error fetching products: " + e.getMessage());
                        return Mono.just(Collections.emptyMap()); // Return empty map if service fails
                    });

            // Fetch all payments
            Mono<Map<String, PaymentView>> paymentMapMono = webClientPaymentServiceGetPayments.get()
                    .retrieve()
                    .bodyToFlux(PaymentView.class)
                    .collectMap(PaymentView::getOrderId)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> {
                        System.err.println("Error fetching payments: " + e.getMessage());
                        return Mono.just(Collections.emptyMap()); // Return empty map if service fails
                    });

            Integer cookie_value =  new Random().nextInt();
            Cookie cookieGetOrderDetails_1 = new Cookie("compositor-service-getorderdeatils-1", cookie_value.toString());
            cookieGetOrderDetails_1.setMaxAge(300);
            boolean success =false;

            return Mono.zip(productMapMono, paymentMapMono)
                    .flatMapMany(tuple -> {
                        Map<String, ProductView> productMap = tuple.getT1();
                        Map<String, PaymentView> paymentMap = tuple.getT2();

                        return orderFlux.map(orderView -> {
                            OrderDetailsView orderDetailsView = new OrderDetailsView();
                            orderDetailsView.setOrderView(orderView);

                            // Check and set ProductView
                            ProductView productView = productMap.get(orderView.getProductId());
                            if (productView == null) {
                                System.err.println("Missing ProductView for Product ID: " + orderView.getProductId());
                            }
                            orderDetailsView.setProductView(productView);

                            // Check and set PaymentView
                            PaymentView paymentView = paymentMap.get(orderView.getOrderId());
                            if (paymentView == null) {
                                System.err.println("Missing PaymentView for Order ID: " + orderView.getOrderId());
                            }
                            orderDetailsView.setPaymentView(paymentView);

                            return orderDetailsView;
                        });
                    })
                    .collectList()
                    .map(orderDetailsList -> {
                        if (orderDetailsList.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No order details available.");
                        }
                        return ResponseEntity.ok(orderDetailsList);
                    })
                    .onErrorResume(e -> {
                        System.err.println("Error during data combination: " + e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("An error occurred during data combination: " + e.getMessage()));
                    });


        }
        else
        {
            // FOLLOW UP REQUEST LOGIC
            // CHECK IF THE COOKIE IS VALID IN REDIS
            // IF VALID, THEN RETURN THE RESPONSE FROM THE FIRST REQUEST
            // IF NOT VALID, THEN RETURN A 400 BAD REQUEST

            Cookie followup_cookie =  cookieList.stream().
                    filter(cookie -> cookie.getName().equals("compositor-service-getorderdeatils-1")).findAny().get();

            String followup_cookie_key = followup_cookie.getName()+followup_cookie.getValue();
            String response = (String)redisTemplate.opsForValue().get(followup_cookie_key);

           /*** if(response == null)
            {
                return ResponseEntity.ok("Request still under process...");
            }
            else if(response.contains("SUCCESS"))
            {
                return ResponseEntity.ok("THIS IS THE ORDER DETAILS YOU WERE LOOKING HERE");
            }
            else if(response.contains("408"))
            {
                return ResponseEntity.ok("Could not connect to Order-Service in time... [WILL RETRY]");
            }
            else if(response.contains("404"))
            {
                return ResponseEntity.ok("Fetching Order Details from Order-Service Failed... [WILL RETRY]");
            }
            else if(response.contains("500"))
            {
                return ResponseEntity.ok("Order-Service messed up... [WILL RETRY]");
            }
            else
            {
                return ResponseEntity.badRequest().body("Something went wrong...");
            } ***/
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList()));

        }

    }


    @GetMapping(value = "allOrderDetails")
    public Flux<OrderDetailsView> fetchAllOrderDetails() {

        Flux<ProductView> productViewFlux = webClientProductServiceGetProducts.get()
                .retrieve()
                .bodyToFlux(ProductView.class); // ASYNCHRONOUS

        Mono<Map<String, ProductView>> productMapMono = productViewFlux.collectMap(ProductView::getProductId);

        Flux<PaymentView> paymentViewFlux = webClientPaymentServiceGetPayments.get()
                .retrieve()
                .bodyToFlux(PaymentView.class); // ASYNCHRONOUS

        Mono<Map<String, PaymentView>> paymentMapMono = paymentViewFlux.collectMap(PaymentView::getOrderId);

        Flux<OrderView> orderViewFlux = webClientOrderServiceGetOrders.get()
                .retrieve()
                .bodyToFlux(OrderView.class); // ASYNCHRONOUS

        return Mono.zip(productMapMono, paymentMapMono)
                .flatMapMany(tuple -> {
                    Map<String, ProductView> productMap = tuple.getT1();
                    Map<String, PaymentView> paymentMap = tuple.getT2();

                    // Enrich each OrderView with ProductView and PaymentView
                    return orderViewFlux.map(orderView -> {
                        OrderDetailsView orderDetailsView = new OrderDetailsView();
                        orderDetailsView.setOrderView(orderView);
                        orderDetailsView.setProductView(productMap.get(orderView.getProductId()));
                        orderDetailsView.setPaymentView(paymentMap.get(orderView.getOrderId()));
                        return orderDetailsView;
                    });
                });
    }

}
