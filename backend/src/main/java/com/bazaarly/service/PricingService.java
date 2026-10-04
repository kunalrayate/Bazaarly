package com.bazaarly.service;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.repo.CouponRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

/** Single source of truth for cart/checkout totals, variants and coupon rules. */
@Service @RequiredArgsConstructor
public class PricingService {
    public static final BigDecimal FREE_SHIPPING_ABOVE = new BigDecimal("500");
    public static final BigDecimal SHIPPING_FEE = new BigDecimal("40");
    private final CouponRepo coupons;

    public record Line(Long cartItemId, Product product, String variantIds, String variantLabel, BigDecimal unitPrice, int quantity, BigDecimal lineTotal, boolean available) {}
    public record Summary(List<Line> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal total, String couponCode, String couponError, int itemCount) {}

    public List<ProductVariant> variantsOf(Product p, String ids) {
        if (ids == null || ids.isBlank()) return List.of();
        Set<Long> set = Arrays.stream(ids.split(",")).filter(s -> !s.isBlank()).map(Long::valueOf).collect(Collectors.toSet());
        return p.getVariants().stream().filter(v -> set.contains(v.getId())).toList();
    }

    public Line line(CartItem c) {
        Product p = c.getProduct();
        List<ProductVariant> vs = variantsOf(p, c.getVariantIds());
        BigDecimal unit = p.getSellingPrice();
        for (ProductVariant v : vs) unit = unit.add(v.getPriceAdjustment() == null ? BigDecimal.ZERO : v.getPriceAdjustment());
        String label = vs.stream().map(v -> v.getType() + ": " + v.getValue()).collect(Collectors.joining(", "));
        boolean ok = p.isActive() && p.getStock() >= c.getQuantity();
        return new Line(c.getId(), p, c.getVariantIds(), label, unit, c.getQuantity(), unit.multiply(BigDecimal.valueOf(c.getQuantity())), ok);
    }

    public Summary summarize(List<CartItem> items, String couponCode) {
        List<Line> lines = items.stream().map(this::line).toList();
        BigDecimal subtotal = lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO; String error = null; String applied = null;
        if (couponCode != null && !couponCode.isBlank()) {
            try { discount = couponDiscount(couponCode, lines, subtotal); applied = couponCode.trim().toUpperCase(); }
            catch (IllegalArgumentException e) { error = e.getMessage(); }
        }
        BigDecimal after = subtotal.subtract(discount);
        BigDecimal shipping = lines.isEmpty() || after.compareTo(FREE_SHIPPING_ABOVE) >= 0 ? BigDecimal.ZERO : SHIPPING_FEE;
        int count = lines.stream().mapToInt(Line::quantity).sum();
        return new Summary(lines, subtotal, discount, shipping, after.add(shipping), applied, error, count);
    }

    /** Throws IllegalArgumentException with a customer-friendly message when the coupon can't be used. */
    public BigDecimal couponDiscount(String code, List<Line> lines, BigDecimal subtotal) {
        Coupon c = coupons.findByCodeIgnoreCase(code.trim()).orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));
        LocalDateTime now = LocalDateTime.now();
        if (!c.isActive()) throw new IllegalArgumentException("This coupon is no longer active");
        if (c.getStartsAt() != null && c.getStartsAt().isAfter(now)) throw new IllegalArgumentException("This coupon is not active yet");
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(now)) throw new IllegalArgumentException("This coupon has expired");
        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit()) throw new IllegalArgumentException("Coupon usage limit reached");
        if (c.getMinPurchase() != null && subtotal.compareTo(c.getMinPurchase()) < 0)
            throw new IllegalArgumentException("Add items worth ₹" + c.getMinPurchase().setScale(0, RoundingMode.UP) + " or more to use this coupon");
        BigDecimal eligible = lines.stream().filter(l -> switch (c.getScope()) {
            case ALL -> true;
            case CATEGORY -> l.product().getCategory() != null && l.product().getCategory().getId().equals(c.getScopeId());
            case PRODUCT -> l.product().getId().equals(c.getScopeId());
        }).map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (eligible.signum() == 0) throw new IllegalArgumentException("This coupon doesn't apply to items in your cart");
        BigDecimal d = c.getType() == CouponType.PERCENT
                ? eligible.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.getValue();
        if (c.getMaxDiscount() != null && d.compareTo(c.getMaxDiscount()) > 0) d = c.getMaxDiscount();
        return d.min(eligible).setScale(2, RoundingMode.HALF_UP);
    }
}
