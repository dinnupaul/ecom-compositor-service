package com.ecom.compositorservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

@Configuration
public class AppConfig {

    @Autowired
    private EurekaDiscoveryClient discoveryClient;

    public ServiceInstance getServiceInstance(String serviceName)
    {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            throw new RuntimeException("No instances found for "+serviceName);
        }
        return instances.get(0); // LOAD BALANCING ALGORITHM WILL GO HERE
    }

    @Bean("product_catalog_service_get_products")
    public WebClient webClientProductCatalogService( WebClient.Builder webClientBuilder)
    {
                ServiceInstance instance = getServiceInstance("product-catalog-service");
                String hostname = instance.getHost();
                int port = instance.getPort();

                return webClientBuilder
                        .baseUrl("http://"+hostname+":"+port+"/api/v1/products/getAll")
                        .filter(new LoggingWebClientFilter())
                        .build();
    }

    @Bean("product_catalog_service_get_all")
    public WebClient webClientProductCatalog( WebClient.Builder webClientBuilder)
    {
        ServiceInstance instance = getServiceInstance("product-catalog-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://"+hostname+":"+port+"/api/v1/products/all")
                .filter(new LoggingWebClientFilter())
                .build();
    }


    @Bean("order_service_get_orders")
    public WebClient webClientOrderService( WebClient.Builder webClientBuilder)
    {
        ServiceInstance instance = getServiceInstance("ecom-order-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://"+hostname+":"+port+"/api/v1/orders/getAll")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean("order_service_get_all")
    public WebClient webClientOrder( WebClient.Builder webClientBuilder)
    {
        ServiceInstance instance = getServiceInstance("ecom-order-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://"+hostname+":"+port+"/api/v1/orders/all")
                .filter(new LoggingWebClientFilter())
                .build();
    }


    @Bean("payment_service_get_payments")
    public WebClient webClientPaymentService( WebClient.Builder webClientBuilder)
    {
        ServiceInstance instance = getServiceInstance("ecom-payment-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://"+hostname+":"+port+"/api/v1/payments/getAll")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean("payment_service_get_all")
    public WebClient webClientPayment( WebClient.Builder webClientBuilder)
    {
        ServiceInstance instance = getServiceInstance("ecom-payment-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://"+hostname+":"+port+"/api/v1/payments/all")
                .filter(new LoggingWebClientFilter())
                .build();
    }


    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000); // 2 seconds

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(150); // Will try for 1 minute

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }


    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(Duration.ofSeconds(60))))
                .build();
    }

}
