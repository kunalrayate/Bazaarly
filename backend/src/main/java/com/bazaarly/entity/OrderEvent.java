package com.bazaarly.entity;

import com.bazaarly.entity.Enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
public class OrderEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private Order order;
    @Enumerated(EnumType.STRING) private OrderStatus status;
    private String note;
    private LocalDateTime time = LocalDateTime.now();
}
