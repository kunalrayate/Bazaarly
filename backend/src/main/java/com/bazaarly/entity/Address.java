package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private User user;
    private String fullName, phone, line1, line2, city, state, pincode;
    private boolean defaultAddress;
    @JsonIgnore public String oneLine() { return fullName + ", " + line1 + (line2 == null || line2.isBlank() ? "" : ", " + line2) + ", " + city + ", " + state + " - " + pincode + " | Ph: " + phone; }
}
