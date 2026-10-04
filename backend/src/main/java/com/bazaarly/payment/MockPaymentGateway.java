package com.bazaarly.payment;

import com.bazaarly.entity.Order;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MockPaymentGateway implements PaymentGateway {
    @Override public Map<String, Object> createPayment(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gateway", "MOCK");
        m.put("paymentRef", "MOCK_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        m.put("amount", o.getTotal());
        m.put("currency", "INR");
        m.put("orderNumber", o.getOrderNumber());
        return m;
    }
    /** Mock rule: the "test checkout" UI posts status=SUCCESS or status=FAILURE. */
    @Override public boolean verify(Order o, Map<String, String> p) {
        return "SUCCESS".equals(p.get("status")) && p.get("paymentRef") != null && p.get("paymentRef").startsWith("MOCK_");
    }
    @Override public String refund(Order o) { return "MOCK_REFUND_" + o.getOrderNumber(); }
}
