package com.bazaarly.entity;

import com.bazaarly.entity.Enums.*;
import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "orders") @Getter @Setter @NoArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true) private String orderNumber;
    @ManyToOne(fetch = FetchType.EAGER) @JsonIgnoreProperties({"hibernateLazyInitializer"}) private User user;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL) private List<OrderItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL) @OrderBy("time ASC") private List<OrderEvent> timeline = new ArrayList<>();
    private String shippingName;
    @Column(length = 600) private String shippingAddress;
    private BigDecimal subtotal, discount, shipping, total;
    private String couponCode;
    private String paymentMethod;   // ONLINE | COD
    @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    private String paymentRef;
    @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.PLACED;
    @Enumerated(EnumType.STRING) private ReturnStatus returnStatus = ReturnStatus.NONE;
    private String returnReason;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime deliveredAt;

    public void addEvent(String note) {
        OrderEvent e = new OrderEvent(); e.setOrder(this); e.setStatus(status); e.setNote(note); timeline.add(e);
    }
}
