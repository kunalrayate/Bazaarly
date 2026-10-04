package com.bazaarly.service;

import com.bazaarly.config.ApiException;
import com.bazaarly.entity.*;
import com.bazaarly.repo.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductService {
    private final ProductRepo products;
    private final CategoryRepo categories;

    public record VariantReq(String type, String value, BigDecimal priceAdjustment) {}
    public record ProductReq(String name, String description, String brand, Long categoryId, BigDecimal price, Integer discountPercent,
                             LocalDateTime discountEndsAt, Integer stock, Integer lowStockThreshold, List<String> images,
                             Map<String, String> specifications, List<VariantReq> variants, Boolean active) {}

    public Page<Product> search(String q, Long categoryId, List<String> brands, BigDecimal min, BigDecimal max, Double minRating,
                                Boolean inStock, String sort, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(root.get("active")));
            if (q != null && !q.isBlank()) {
                for (String term : q.trim().toLowerCase().split("\\s+")) {
                    String like = "%" + term + "%";
                    Join<Object, Object> cat = root.join("category", JoinType.LEFT);
                    ps.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("brand")), like),
                            cb.like(cb.lower(cat.get("name")), like), cb.like(cb.lower(root.get("description")), like)));
                }
            }
            if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
            if (brands != null && !brands.isEmpty()) ps.add(root.get("brand").in(brands));
            if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), min));
            if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), max));
            if (minRating != null) ps.add(cb.greaterThanOrEqualTo(root.get("ratingAvg"), minRating));
            if (Boolean.TRUE.equals(inStock)) ps.add(cb.greaterThan(root.get("stock"), 0));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort s = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("sellingPrice").ascending();
            case "price_desc" -> Sort.by("sellingPrice").descending();
            case "rating" -> Sort.by("ratingAvg").descending().and(Sort.by("ratingCount").descending());
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("soldCount").descending().and(Sort.by("viewCount").descending()); // popularity / relevance
        };
        return products.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 60), s));
    }

    public Product apply(Product p, ProductReq r) {
        if (r.name() != null) p.setName(r.name().trim());
        if (r.description() != null) p.setDescription(r.description());
        if (r.brand() != null) p.setBrand(r.brand().trim());
        if (r.categoryId() != null) p.setCategory(categories.findById(r.categoryId()).orElseThrow(() -> ApiException.bad("Unknown category")));
        if (r.price() != null) {
            if (r.price().signum() < 0) throw ApiException.bad("Price cannot be negative");
            p.setPrice(r.price());
        }
        if (r.discountPercent() != null) {
            if (r.discountPercent() < 0 || r.discountPercent() > 90) throw ApiException.bad("Discount must be between 0 and 90%");
            p.setDiscountPercent(r.discountPercent());
        }
        p.setDiscountEndsAt(r.discountEndsAt());
        if (r.stock() != null) { if (r.stock() < 0) throw ApiException.bad("Stock cannot be negative"); p.setStock(r.stock()); }
        if (r.lowStockThreshold() != null) p.setLowStockThreshold(r.lowStockThreshold());
        if (r.images() != null) p.setImages(new ArrayList<>(r.images().stream().filter(s -> s != null && !s.isBlank()).toList()));
        if (r.specifications() != null) p.setSpecifications(new LinkedHashMap<>(r.specifications()));
        if (r.variants() != null) {
            p.getVariants().clear();
            for (VariantReq v : r.variants()) {
                if (v.type() == null || v.type().isBlank() || v.value() == null || v.value().isBlank()) continue;
                ProductVariant pv = new ProductVariant(); pv.setProduct(p); pv.setType(v.type().trim()); pv.setValue(v.value().trim());
                pv.setPriceAdjustment(v.priceAdjustment() == null ? BigDecimal.ZERO : v.priceAdjustment()); p.getVariants().add(pv);
            }
        }
        if (r.active() != null) p.setActive(r.active());
        if (p.getName() == null || p.getName().isBlank()) throw ApiException.bad("Product name is required");
        if (p.getCategory() == null) throw ApiException.bad("Category is required");
        p.computePrice();
        return products.save(p);
    }
}
