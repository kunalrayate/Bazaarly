package com.bazaarly.service;

import com.bazaarly.config.ApiException;
import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.payment.PaymentGateway;
import com.bazaarly.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class OrderService {
    private static final List<OrderStatus> FLOW = List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);
    public static final int RETURN_WINDOW_DAYS = 7;

    private final OrderRepo orders;
    private final CartRepo cart;
    private final AddressRepo addresses;
    private final ProductRepo products;
    private final CouponRepo coupons;
    private final PricingService pricing;
    private final NotificationService notifications;
    private final PaymentGateway gateway;

    @Transactional
    public Order place(User u, Long addressId, String couponCode, String method) {
        List<CartItem> items = cart.findByUserIdOrderByIdAsc(u.getId());
        if (items.isEmpty()) throw ApiException.bad("Your cart is empty");
        Address a = addresses.findByIdAndUserId(addressId, u.getId()).orElseThrow(() -> ApiException.bad("Please select a delivery address"));
        if (!"ONLINE".equals(method) && !"COD".equals(method)) throw ApiException.bad("Invalid payment method");

        // lock products in a stable order, validate & deduct stock
        items.sort(Comparator.comparing(i -> i.getProduct().getId()));
        Order o = new Order(); o.setUser(u);
        for (CartItem ci : items) {
            Product p = products.lockById(ci.getProduct().getId()).orElseThrow();
            if (!p.isActive()) throw ApiException.bad(p.getName() + " is no longer available");
            if (p.getStock() < ci.getQuantity()) throw ApiException.bad("Only " + p.getStock() + " left of " + p.getName());
            p.setStock(p.getStock() - ci.getQuantity()); p.setSoldCount(p.getSoldCount() + ci.getQuantity()); products.save(p);
            PricingService.Line l = pricing.line(ci);
            OrderItem oi = new OrderItem(); oi.setOrder(o); oi.setProduct(p); oi.setProductName(p.getName());
            oi.setImage(p.getImages().isEmpty() ? null : p.getImages().get(0)); oi.setVariantLabel(l.variantLabel());
            oi.setUnitPrice(l.unitPrice()); oi.setQuantity(ci.getQuantity()); o.getItems().add(oi);
        }
        PricingService.Summary s = pricing.summarize(items, couponCode);
        if (s.couponError() != null) throw ApiException.bad(s.couponError());
        if (s.couponCode() != null) coupons.findByCodeIgnoreCase(s.couponCode()).ifPresent(c -> { c.setUsedCount(c.getUsedCount() + 1); coupons.save(c); });

        o.setOrderNumber("BZ" + LocalDate.now().toString().replace("-", "") + (10000 + new Random().nextInt(90000)));
        o.setShippingName(a.getFullName()); o.setShippingAddress(a.oneLine());
        o.setSubtotal(s.subtotal()); o.setDiscount(s.discount()); o.setShipping(s.shipping()); o.setTotal(s.total());
        o.setCouponCode(s.couponCode()); o.setPaymentMethod(method); o.setStatus(OrderStatus.PLACED);
        o.addEvent("Order placed");
        orders.save(o);
        cart.deleteByUserId(u.getId());

        notifications.notify(u, "Order placed", "Your order " + o.getOrderNumber() + " was placed successfully.");
        Set<User> sellers = new LinkedHashSet<>(); o.getItems().forEach(i -> sellers.add(i.getProduct().getSeller()));
        sellers.stream().filter(Objects::nonNull).forEach(sl -> notifications.notify(sl, "New order", "New order " + o.getOrderNumber() + " needs confirmation."));
        o.getItems().stream().map(OrderItem::getProduct).filter(p -> p.getStock() <= p.getLowStockThreshold() && p.getSeller() != null).distinct()
                .forEach(p -> notifications.notify(p.getSeller(), "Low stock", p.getName() + " has only " + p.getStock() + " units left."));
        return o;
    }

    // ---------------- payments ----------------
    @Transactional
    public Map<String, Object> initiatePayment(User u, Long orderId) {
        Order o = orders.findByIdAndUserId(orderId, u.getId()).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getPaymentStatus() == PaymentStatus.PAID) throw ApiException.bad("Order is already paid");
        if (o.getStatus() == OrderStatus.CANCELLED) throw ApiException.bad("Order is cancelled");
        Map<String, Object> init = gateway.createPayment(o);
        o.setPaymentRef(String.valueOf(init.get("paymentRef"))); orders.save(o);
        return init;
    }

    @Transactional
    public Order confirmPayment(User u, Long orderId, Map<String, String> payload) {
        Order o = orders.findByIdAndUserId(orderId, u.getId()).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getPaymentStatus() == PaymentStatus.PAID) return o;
        if (gateway.verify(o, payload)) {
            o.setPaymentStatus(PaymentStatus.PAID); o.setPaymentRef(payload.getOrDefault("paymentRef", o.getPaymentRef()));
            if (o.getStatus() == OrderStatus.PLACED) o.setStatus(OrderStatus.CONFIRMED);
            o.addEvent("Payment received - order confirmed");
            notifications.notify(u, "Payment successful", "We received ₹" + o.getTotal() + " for order " + o.getOrderNumber() + ".");
        } else {
            o.setPaymentStatus(PaymentStatus.FAILED); o.addEvent("Payment failed");
            notifications.notify(u, "Payment failed", "Payment for order " + o.getOrderNumber() + " failed. You can retry from My Orders.");
        }
        return orders.save(o);
    }

    // ---------------- lifecycle ----------------
    @Transactional
    public Order updateStatus(Order o, OrderStatus next) {
        OrderStatus cur = o.getStatus();
        if (next == OrderStatus.CANCELLED) {
            if (FLOW.indexOf(cur) > FLOW.indexOf(OrderStatus.PROCESSING) || cur == OrderStatus.CANCELLED) throw ApiException.bad("Order can no longer be cancelled");
            return cancelInternal(o, "Cancelled by seller/admin");
        }
        int ci = FLOW.indexOf(cur), ni = FLOW.indexOf(next);
        if (ci < 0 || ni < 0 || ni <= ci) throw ApiException.bad("Invalid status change from " + cur + " to " + next);
        if ("ONLINE".equals(o.getPaymentMethod()) && o.getPaymentStatus() != PaymentStatus.PAID) throw ApiException.bad("Online payment is still pending for this order");
        o.setStatus(next);
        if (next == OrderStatus.DELIVERED) {
            o.setDeliveredAt(LocalDateTime.now());
            if ("COD".equals(o.getPaymentMethod())) o.setPaymentStatus(PaymentStatus.PAID);
        }
        o.addEvent(label(next));
        notifications.notify(o.getUser(), "Order " + label(next).toLowerCase(), "Order " + o.getOrderNumber() + ": " + label(next) + ".");
        return orders.save(o);
    }

    @Transactional
    public Order cancelByCustomer(User u, Long orderId) {
        Order o = orders.findByIdAndUserId(orderId, u.getId()).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getStatus() == OrderStatus.CANCELLED) throw ApiException.bad("Order is already cancelled");
        if (FLOW.indexOf(o.getStatus()) > FLOW.indexOf(OrderStatus.PROCESSING)) throw ApiException.bad("This order has already been shipped and can't be cancelled. You can request a return after delivery.");
        return cancelInternal(o, "Cancelled by customer");
    }

    private Order cancelInternal(Order o, String note) {
        restock(o);
        if (o.getCouponCode() != null) coupons.findByCodeIgnoreCase(o.getCouponCode()).ifPresent(c -> { c.setUsedCount(Math.max(0, c.getUsedCount() - 1)); coupons.save(c); });
        o.setStatus(OrderStatus.CANCELLED);
        if (o.getPaymentStatus() == PaymentStatus.PAID) { gateway.refund(o); o.setPaymentStatus(PaymentStatus.REFUNDED); }
        o.addEvent(note);
        notifications.notify(o.getUser(), "Order cancelled", "Order " + o.getOrderNumber() + " was cancelled." + (o.getPaymentStatus() == PaymentStatus.REFUNDED ? " Your refund has been initiated." : ""));
        return orders.save(o);
    }

    private void restock(Order o) {
        for (OrderItem i : o.getItems()) {
            products.lockById(i.getProduct().getId()).ifPresent(p -> { p.setStock(p.getStock() + i.getQuantity()); p.setSoldCount(Math.max(0, p.getSoldCount() - i.getQuantity())); products.save(p); });
        }
    }

    @Transactional
    public Order requestReturn(User u, Long orderId, String reason) {
        Order o = orders.findByIdAndUserId(orderId, u.getId()).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getStatus() != OrderStatus.DELIVERED) throw ApiException.bad("Only delivered orders can be returned");
        if (o.getReturnStatus() != ReturnStatus.NONE) throw ApiException.bad("A return was already requested for this order");
        if (o.getDeliveredAt() != null && o.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS).isBefore(LocalDateTime.now())) throw ApiException.bad("The " + RETURN_WINDOW_DAYS + "-day return window has passed");
        if (reason == null || reason.isBlank()) throw ApiException.bad("Please tell us why you are returning this order");
        o.setReturnStatus(ReturnStatus.REQUESTED); o.setReturnReason(reason.trim()); o.addEvent("Return requested");
        Set<User> sellers = new LinkedHashSet<>(); o.getItems().forEach(i -> sellers.add(i.getProduct().getSeller()));
        sellers.stream().filter(Objects::nonNull).forEach(s -> notifications.notify(s, "Return requested", "Return requested for order " + o.getOrderNumber() + "."));
        notifications.notifyAdmins("Return requested", "Return requested for order " + o.getOrderNumber() + ".");
        return orders.save(o);
    }

    @Transactional
    public Order decideReturn(Order o, boolean approve) {
        if (o.getReturnStatus() != ReturnStatus.REQUESTED) throw ApiException.bad("No pending return request");
        if (approve) {
            restock(o); o.setReturnStatus(ReturnStatus.REFUNDED); o.setStatus(OrderStatus.RETURNED);
            if (o.getPaymentStatus() == PaymentStatus.PAID) { gateway.refund(o); o.setPaymentStatus(PaymentStatus.REFUNDED); }
            o.addEvent("Return approved - refund initiated");
            notifications.notify(o.getUser(), "Return approved", "Your return for " + o.getOrderNumber() + " was approved. Refund of ₹" + o.getTotal() + " initiated.");
        } else {
            o.setReturnStatus(ReturnStatus.REJECTED); o.addEvent("Return rejected");
            notifications.notify(o.getUser(), "Return rejected", "Your return request for " + o.getOrderNumber() + " was not approved.");
        }
        return orders.save(o);
    }

    public static String label(OrderStatus s) {
        return switch (s) { case PLACED -> "Order Placed"; case CONFIRMED -> "Confirmed"; case PROCESSING -> "Processing"; case SHIPPED -> "Shipped";
            case OUT_FOR_DELIVERY -> "Out for Delivery"; case DELIVERED -> "Delivered"; case CANCELLED -> "Cancelled"; case RETURNED -> "Returned"; };
    }
}
