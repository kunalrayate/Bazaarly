package com.bazaarly.entity;

import com.bazaarly.entity.Enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(unique = true, nullable = false) private String email;
    @JsonIgnore private String password;
    private String phone;
    @Enumerated(EnumType.STRING) private Role role = Role.CUSTOMER;
    @Enumerated(EnumType.STRING) private UserStatus status = UserStatus.ACTIVE;
    // seller profile
    private String storeName;
    @Column(length = 1000) private String storeDescription;
    private String businessAddress;
    private String gstin;
    private LocalDateTime createdAt = LocalDateTime.now();
}
