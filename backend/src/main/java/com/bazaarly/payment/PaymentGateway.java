package com.bazaarly.payment;

import com.bazaarly.entity.Order;
import java.util.Map;

/**
 * Payment abstraction. Today: MockPaymentGateway. To go live with Razorpay, add a class
 * RazorpayPaymentGateway implements PaymentGateway, annotate it @Service, and remove @Service from the mock
 * (see README -> "Switching to Razorpay").
 */
public interface PaymentGateway {
    /** Creates a gateway-side payment/order. The map is returned to the browser to start checkout. */
    Map<String, Object> createPayment(Order order);
    /** Verifies the payload posted back by the browser after checkout (e.g. Razorpay signature). */
    boolean verify(Order order, Map<String, String> payload);
    /** Refund a captured payment. Return a refund reference. */
    String refund(Order order);
}
