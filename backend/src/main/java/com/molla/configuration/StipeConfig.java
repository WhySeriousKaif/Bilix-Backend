package com.molla.configuration;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StipeConfig {

    @Value("${stripe.secret.key}")
    private  String stipeSecretKey;

    @PostConstruct
    public void init(){
        Stripe.apiKey=stipeSecretKey;
    }
}
