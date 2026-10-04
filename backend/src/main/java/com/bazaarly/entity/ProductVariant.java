package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** One selectable option, e.g. type="Color", value="Black", or type="Storage", value="256 GB" (+₹5000). */
@Entity @Getter @Setter @NoArgsConstructor
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private Product product;
    private String type;
    private String value;
    private BigDecimal priceAdjustment = BigDecimal.ZERO;
}
