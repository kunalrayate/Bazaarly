package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.Role;
import com.bazaarly.repo.*;
import com.bazaarly.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class CatalogController {
    private final ProductRepo products;
    private final CategoryRepo categories;
    private final CouponRepo coupons;
    private final RecentViewRepo views;
    private final ProductService productService;
    private final RecommendationService recs;

    @GetMapping("/home")
    public Map<String, Object> home() {
        User u = Auth.current(); Map<String, Object> m = new LinkedHashMap<>();
        m.put("categories", categories.findAll());
        m.put("featured", products.findByActiveTrueAndFeaturedTrueOrderByIdDesc(PageRequest.of(0, 10)));
        m.put("trending", products.findByActiveTrueOrderByViewCountDesc(PageRequest.of(0, 10)));
        m.put("bestSellers", products.findByActiveTrueOrderBySoldCountDesc(PageRequest.of(0, 10)));
        m.put("newArrivals", products.findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, 10)));
        m.put("deals", products.findByActiveTrueAndDiscountPercentGreaterThanOrderByDiscountPercentDesc(0, PageRequest.of(0, 10)));
        m.put("recommended", recs.forUser(u, 10));
        m.put("promotions", coupons.findLive(LocalDateTime.now()));
        return m;
    }

    @GetMapping("/categories") public List<Category> categories() { return categories.findAll(); }
    @GetMapping("/coupons/active") public List<Coupon> activeCoupons() { return coupons.findLive(LocalDateTime.now()); }
    @GetMapping("/recommendations") public List<Product> recommendations() { return recs.forUser(Auth.current(), 12); }

    @GetMapping("/products")
    public Map<String, Object> search(@RequestParam(required = false) String q, @RequestParam(required = false) Long category,
                                      @RequestParam(required = false) List<String> brand, @RequestParam(required = false) BigDecimal minPrice,
                                      @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) Double minRating,
                                      @RequestParam(required = false) Boolean inStock, @RequestParam(required = false) String sort,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        Page<Product> p = productService.search(q, category, brand, minPrice, maxPrice, minRating, inStock, sort, page, size);
        return Map.of("content", p.getContent(), "page", p.getNumber(), "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements());
    }

    @GetMapping("/products/brands") public List<String> brands(@RequestParam(required = false) Long category) { return products.brands(category); }

    @GetMapping("/products/{id}")
    public Product detail(@PathVariable Long id) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        User u = Auth.current();
        boolean privileged = u != null && (u.getRole() == Role.ADMIN || (p.getSeller() != null && p.getSeller().getId().equals(u.getId())));
        if (!p.isActive() && !privileged) throw ApiException.notFound("Product not found");
        p.setViewCount(p.getViewCount() + 1); products.save(p);
        if (u != null && u.getRole() == Role.CUSTOMER) {
            RecentView rv = views.findByUserIdAndProductId(u.getId(), id).orElseGet(() -> { RecentView n = new RecentView(); n.setUser(u); n.setProduct(p); return n; });
            rv.setViewedAt(LocalDateTime.now()); views.save(rv);
        }
        return p;
    }

    @GetMapping("/products/{id}/similar")
    public List<Product> similar(@PathVariable Long id) { return recs.similar(products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found")), 8); }

    @GetMapping("/products/{id}/frequently-bought") public List<Product> fbt(@PathVariable Long id) { return recs.frequentlyBought(id, 4); }

    @GetMapping("/products/recently-viewed")
    public List<Product> recentlyViewed() {
        User u = Auth.current(); if (u == null) return List.of();
        return views.findTop20ByUserIdOrderByViewedAtDesc(u.getId()).stream().map(RecentView::getProduct).filter(Product::isActive).limit(10).toList();
    }
}
