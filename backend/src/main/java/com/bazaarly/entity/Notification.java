package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private User user;
    private String title;
    @Column(length = 500) private String message;
    @Column(name = "is_read") private boolean read;
    private LocalDateTime createdAt = LocalDateTime.now();
}
