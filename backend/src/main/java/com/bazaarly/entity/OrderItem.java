package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private Order order;
    @ManyToOne(fetch = FetchType.EAGER) private Product product;
    private String productName, image, variantLabel;
    private BigDecimal unitPrice;
    private int quantity;
    @JsonProperty("lineTotal") public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
