package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(indexes = {@Index(columnList = "name"), @Index(columnList = "brand"), @Index(columnList = "selling_price")})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    private String brand;
    @ManyToOne(fetch = FetchType.EAGER) private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private User seller;
    private BigDecimal price = BigDecimal.ZERO;            // MRP
    private int discountPercent;                           // product-specific offer
    private LocalDateTime discountEndsAt;                  // limited-time deal (null = no expiry)
    private BigDecimal sellingPrice = BigDecimal.ZERO;     // persisted so we can filter/sort in SQL
    private int stock;
    private int lowStockThreshold = 10;
    @ElementCollection @CollectionTable(name = "product_images") @OrderColumn private List<String> images = new ArrayList<>();
    @ElementCollection @CollectionTable(name = "product_specs") @MapKeyColumn(name = "spec_key") @Column(name = "spec_value")
    private Map<String, String> specifications = new LinkedHashMap<>();
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true) private List<ProductVariant> variants = new ArrayList<>();
    private double ratingAvg;
    private int ratingCount;
    private int soldCount;
    private int viewCount;
    private boolean featured;
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist @PreUpdate public void computePrice() {
        BigDecimal off = price.multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        sellingPrice = price.subtract(off);
    }
    @JsonProperty("lowStock") public boolean isLowStock() { return stock <= lowStockThreshold; }
    @JsonProperty("sellerId") public Long sellerId() { return seller == null ? null : seller.getId(); }
    @JsonProperty("sellerName") public String sellerName() { return seller == null ? null : (seller.getStoreName() != null ? seller.getStoreName() : seller.getName()); }
}
