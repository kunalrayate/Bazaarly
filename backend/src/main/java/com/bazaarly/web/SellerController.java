package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.repo.*;
import com.bazaarly.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/seller") @RequiredArgsConstructor
public class SellerController {
    private final ProductRepo products;
    private final OrderRepo orders;
    private final ProductService productService;
    private final OrderService orderService;
    private final StatsService stats;

    public record StatusReq(OrderStatus status) {}
    public record StockReq(int stock) {}
    public record DecisionReq(boolean approve) {}

    @GetMapping("/stats") public Map<String, Object> stats() { return stats.seller(Auth.require()); }

    @GetMapping("/products")
    public Map<String, Object> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Product> p = products.searchBySeller(Auth.require().getId(), q, PageRequest.of(page, size, Sort.by("id").descending()));
        return Map.of("content", p.getContent(), "page", p.getNumber(), "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements());
    }

    @PostMapping("/products")
    public Product create(@RequestBody ProductService.ProductReq r) { Product p = new Product(); p.setSeller(Auth.require()); return productService.apply(p, r); }

    @PutMapping("/products/{id}")
    public Product update(@PathVariable Long id, @RequestBody ProductService.ProductReq r) { return productService.apply(owned(id), r); }

    @PutMapping("/products/{id}/stock")
    public Product stock(@PathVariable Long id, @RequestBody StockReq r) {
        if (r.stock() < 0) throw ApiException.bad("Stock cannot be negative");
        Product p = owned(id); p.setStock(r.stock()); return products.save(p);
    }

    /** Soft delete: keeps order history intact. */
    @DeleteMapping("/products/{id}")
    public Map<String, String> delete(@PathVariable Long id) { Product p = owned(id); p.setActive(false); products.save(p); return Map.of("message", "Product unlisted"); }

    @GetMapping("/orders")
    public Map<String, Object> orders(@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page) {
        Page<Order> p = orders.findBySeller(Auth.require().getId(), status, PageRequest.of(page, 10, Sort.by("createdAt").descending()));
        return Map.of("content", p.getContent(), "page", p.getNumber(), "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements());
    }

    @PutMapping("/orders/{id}/status")
    public Order status(@PathVariable Long id, @RequestBody StatusReq r) { return orderService.updateStatus(ownedOrder(id), r.status()); }

    @PostMapping("/orders/{id}/return")
    public Order decide(@PathVariable Long id, @RequestBody DecisionReq r) { return orderService.decideReturn(ownedOrder(id), r.approve()); }

    private Product owned(Long id) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (p.getSeller() == null || !p.getSeller().getId().equals(Auth.require().getId())) throw ApiException.forbidden("This is not your product");
        return p;
    }
    private Order ownedOrder(Long id) {
        if (!orders.sellerOwns(id, Auth.require().getId())) throw ApiException.forbidden("This order has none of your products");
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
    }
}
