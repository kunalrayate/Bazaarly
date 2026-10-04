package com.bazaarly.entity;

import com.bazaarly.entity.Enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String code;
    private String description;
    @Enumerated(EnumType.STRING) private CouponType type = CouponType.PERCENT;
    private BigDecimal value = BigDecimal.ZERO;
    private BigDecimal minPurchase = BigDecimal.ZERO;
    private BigDecimal maxDiscount;                    // cap for percentage coupons (nullable)
    @Enumerated(EnumType.STRING) private CouponScope scope = CouponScope.ALL;
    private Long scopeId;                              // category id or product id
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Integer usageLimit;
    private int usedCount;
    private boolean active = true;
}
