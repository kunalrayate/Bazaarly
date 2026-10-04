package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private User user;
    @ManyToOne(fetch = FetchType.EAGER) private Product product;
    private String variantIds = ""; // comma separated ProductVariant ids
    private int quantity = 1;
}
