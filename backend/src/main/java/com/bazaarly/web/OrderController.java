package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.repo.OrderRepo;
import com.bazaarly.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class OrderController {
    private final OrderService service;
    private final OrderRepo orders;

    public record PlaceReq(Long addressId, String couponCode, String paymentMethod) {}
    public record ReasonReq(String reason) {}

    @PostMapping("/orders") public Order place(@RequestBody PlaceReq r) { return service.place(Auth.require(), r.addressId(), r.couponCode(), r.paymentMethod()); }
    @GetMapping("/orders") public List<Order> mine() { return orders.findByUserIdOrderByCreatedAtDesc(Auth.require().getId()); }
    @GetMapping("/orders/{id}") public Order one(@PathVariable Long id) { return orders.findByIdAndUserId(id, Auth.require().getId()).orElseThrow(() -> ApiException.notFound("Order not found")); }
    @PostMapping("/orders/{id}/cancel") public Order cancel(@PathVariable Long id) { return service.cancelByCustomer(Auth.require(), id); }
    @PostMapping("/orders/{id}/return") public Order ret(@PathVariable Long id, @RequestBody ReasonReq r) { return service.requestReturn(Auth.require(), id, r.reason()); }

    @PostMapping("/payments/{orderId}/initiate") public Map<String, Object> initiate(@PathVariable Long orderId) { return service.initiatePayment(Auth.require(), orderId); }
    @PostMapping("/payments/{orderId}/confirm") public Order confirm(@PathVariable Long orderId, @RequestBody Map<String, String> payload) { return service.confirmPayment(Auth.require(), orderId, payload); }
}
