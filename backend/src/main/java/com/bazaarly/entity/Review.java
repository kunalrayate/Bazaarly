package com.bazaarly.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JsonIgnore private User user;
    @ManyToOne(fetch = FetchType.EAGER) @JsonIgnoreProperties({"description", "specifications", "variants", "images"}) private Product product;
    private int rating;
    private String title;
    @Column(length = 2000) private String comment;
    private boolean reported;
    private String reportReason;
    private boolean hidden;
    private LocalDateTime createdAt = LocalDateTime.now();
    @JsonProperty("userName") public String userName() { return user == null ? "Anonymous" : user.getName(); }
}
