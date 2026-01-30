
package com.eazybytes.eazystore.service.impl;

import com.eazybytes.eazystore.dto.PaymentIntentRequestDto;
import com.eazybytes.eazystore.dto.PaymentIntentResponseDto;
import com.eazybytes.eazystore.service.IPaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// @Service
// public class PaymentServiceImpl implements IPaymentService {

//     @Value("${stripe.apiKey}")
//     private String stripeApiKey;

//     @PostConstruct
//     public void init() {
//         Stripe.apiKey = stripeApiKey;
//     }

//     @Override
//     public PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto) {
//         try {
//             PaymentIntentCreateParams params =
//                     PaymentIntentCreateParams.builder()
//                             .setAmount(requestDto.amount()) // IMPORTANT
//                             .setCurrency(requestDto.currency())
//                             .addPaymentMethodType("card")
//                             .build();

//             PaymentIntent paymentIntent = PaymentIntent.create(params);
//             return new PaymentIntentResponseDto(paymentIntent.getClientSecret());

//         } catch (StripeException e) {
//             e.printStackTrace();
//             throw new RuntimeException("Failed to create payment intent", e);
//         }
//     }
// }


@Service
public class PaymentServiceImpl implements IPaymentService {

    @Override
    public PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto) {
        try {
            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(requestDto.amount())   // FIXED
                            .setCurrency(requestDto.currency())
                            .addPaymentMethodType("card")
                            .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return new PaymentIntentResponseDto(paymentIntent.getClientSecret());

        } catch (StripeException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create payment intent", e);
        }
    }
}
