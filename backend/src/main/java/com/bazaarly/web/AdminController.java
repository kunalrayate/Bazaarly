package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.repo.*;
import com.bazaarly.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminController {
    private final UserRepo users;
    private final CategoryRepo categories;
    private final ProductRepo products;
    private final OrderRepo orders;
    private final CouponRepo coupons;
    private final ReviewRepo reviews;
    private final StatsService stats;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final ProductService productService;
    private final NotificationService notifications;

    public record StatusReq(UserStatus status) {}
    public record OrderStatusReq(OrderStatus status) {}
    public record DecisionReq(boolean approve) {}
    public record FlagsReq(Boolean featured, Boolean active) {}

    private Map<String, Object> page(Page<?> p) { return Map.of("content", p.getContent(), "page", p.getNumber(), "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements()); }

    @GetMapping("/stats") public Map<String, Object> stats() { return stats.admin(); }

    // ---- users & sellers
    @GetMapping("/users")
    public Map<String, Object> users(@RequestParam(defaultValue = "CUSTOMER") Role role, @RequestParam(required = false) UserStatus status,
                                     @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
        return page(users.search(role, status, q, PageRequest.of(page, 15, Sort.by("id").descending())));
    }

    @PutMapping("/users/{id}/status")
    public User setStatus(@PathVariable Long id, @RequestBody StatusReq r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getRole() == Role.ADMIN) throw ApiException.forbidden("Admin accounts cannot be modified");
        if (r.status() == UserStatus.PENDING) throw ApiException.bad("Invalid status");
        UserStatus before = u.getStatus(); u.setStatus(r.status()); users.save(u);
        if (u.getRole() == Role.SELLER && before == UserStatus.PENDING)
            notifications.notify(u, r.status() == UserStatus.ACTIVE ? "Store approved" : "Store rejected", r.status() == UserStatus.ACTIVE ? "Your seller account is approved. Start listing products!" : "Your seller registration was rejected.");
        return u;
    }

    // ---- categories
    @GetMapping("/categories") public List<Category> categories() { return categories.findAll(); }
    @PostMapping("/categories") public Category addCat(@RequestBody Category c) { c.setId(null); return save(c); }
    @PutMapping("/categories/{id}") public Category editCat(@PathVariable Long id, @RequestBody Category c) { c.setId(id); return save(c); }
    @DeleteMapping("/categories/{id}")
    public Map<String, String> delCat(@PathVariable Long id) {
        try { categories.deleteById(id); categories.flush(); } catch (DataIntegrityViolationException e) { throw ApiException.bad("Category has products - move or remove them first"); }
        return Map.of("message", "Deleted");
    }
    private Category save(Category c) {
        if (c.getName() == null || c.getName().isBlank()) throw ApiException.bad("Category name is required");
        try { return categories.save(c); } catch (DataIntegrityViolationException e) { throw ApiException.bad("A category with this name already exists"); }
    }

    // ---- products / listings / inventory
    @GetMapping("/products")
    public Map<String, Object> products(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
        return page(products.findAll((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"), cb.like(cb.lower(root.get("brand")), "%" + q.toLowerCase() + "%")),
                PageRequest.of(page, 15, Sort.by("id").descending())));
    }
    @PutMapping("/products/{id}/flags")
    public Product flags(@PathVariable Long id, @RequestBody FlagsReq r) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (r.featured() != null) p.setFeatured(r.featured()); if (r.active() != null) p.setActive(r.active()); return products.save(p);
    }
    @PutMapping("/products/{id}")
    public Product edit(@PathVariable Long id, @RequestBody ProductService.ProductReq r) { return productService.apply(products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found")), r); }
    @GetMapping("/inventory/low-stock") public List<Product> lowStock() { return products.lowStock(PageRequest.of(0, 100)); }

    // ---- orders
    @GetMapping("/orders")
    public Map<String, Object> orders(@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page) {
        PageRequest pr = PageRequest.of(page, 15, Sort.by("createdAt").descending());
        return page(status == null ? orders.findAll(pr) : orders.findByStatus(status, pr));
    }
    @PutMapping("/orders/{id}/status")
    public Order orderStatus(@PathVariable Long id, @RequestBody OrderStatusReq r) { return orderService.updateStatus(order(id), r.status()); }
    @PostMapping("/orders/{id}/return")
    public Order decide(@PathVariable Long id, @RequestBody DecisionReq r) { return orderService.decideReturn(order(id), r.approve()); }
    private Order order(Long id) { return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found")); }

    // ---- coupons / offers
    @GetMapping("/coupons") public List<Coupon> coupons() { return coupons.findAll(Sort.by("id").descending()); }
    @PostMapping("/coupons") public Coupon addCoupon(@RequestBody Coupon c) { c.setId(null); c.setUsedCount(0); return saveCoupon(c); }
    @PutMapping("/coupons/{id}")
    public Coupon editCoupon(@PathVariable Long id, @RequestBody Coupon c) {
        Coupon old = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Coupon not found")); c.setId(id); c.setUsedCount(old.getUsedCount()); return saveCoupon(c);
    }
    @DeleteMapping("/coupons/{id}") public Map<String, String> delCoupon(@PathVariable Long id) { coupons.deleteById(id); return Map.of("message", "Deleted"); }
    private Coupon saveCoupon(Coupon c) {
        if (c.getCode() == null || c.getCode().isBlank()) throw ApiException.bad("Coupon code is required");
        if (c.getValue() == null || c.getValue().signum() <= 0) throw ApiException.bad("Coupon value must be positive");
        if (c.getType() == CouponType.PERCENT && c.getValue().intValue() > 100) throw ApiException.bad("Percentage cannot exceed 100");
        if (c.getScope() != CouponScope.ALL && c.getScopeId() == null) throw ApiException.bad("Select the category/product id this coupon applies to");
        c.setCode(c.getCode().trim().toUpperCase());
        try { return coupons.save(c); } catch (DataIntegrityViolationException e) { throw ApiException.bad("A coupon with this code already exists"); }
    }

    // ---- reviews & reported content
    @GetMapping("/reviews")
    public Map<String, Object> reviews(@RequestParam(defaultValue = "true") boolean reportedOnly, @RequestParam(defaultValue = "0") int page) {
        if (reportedOnly) { var l = reviews.findByReportedTrueAndHiddenFalseOrderByCreatedAtDesc(); return Map.of("content", l, "page", 0, "totalPages", 1, "totalElements", l.size()); }
        return page(reviews.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 15)));
    }
    @PostMapping("/reviews/{id}/hide")
    public Map<String, String> hide(@PathVariable Long id) {
        Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Review not found")); r.setHidden(true); reviews.save(r); reviewService.recalc(r.getProduct()); return Map.of("message", "Review hidden");
    }
    @PostMapping("/reviews/{id}/dismiss")
    public Map<String, String> dismiss(@PathVariable Long id) {
        Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Review not found")); r.setReported(false); r.setReportReason(null); reviews.save(r); return Map.of("message", "Report dismissed");
    }
}
