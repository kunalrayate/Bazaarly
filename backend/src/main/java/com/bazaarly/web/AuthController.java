package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.repo.UserRepo;
import com.bazaarly.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepo users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final NotificationService notifications;

    public record RegisterReq(@NotBlank String name, @Email @NotBlank String email, @Size(min = 6, message = "must be at least 6 characters") String password, String phone) {}
    public record SellerReq(@NotBlank String name, @Email @NotBlank String email, @Size(min = 6, message = "must be at least 6 characters") String password, String phone,
                            @NotBlank String storeName, String storeDescription, String businessAddress, String gstin) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterReq r) {
        User u = create(r.name(), r.email(), r.password(), r.phone(), Role.CUSTOMER, UserStatus.ACTIVE);
        notifications.notify(u, "Welcome to Bazaarly!", "Start exploring thousands of products. Use WELCOME10 on your first order.");
        return Map.of("token", jwt.generate(u), "user", u);
    }

    @PostMapping("/register-seller")
    public Map<String, String> registerSeller(@Valid @RequestBody SellerReq r) {
        User u = create(r.name(), r.email(), r.password(), r.phone(), Role.SELLER, UserStatus.PENDING);
        u.setStoreName(r.storeName()); u.setStoreDescription(r.storeDescription()); u.setBusinessAddress(r.businessAddress()); u.setGstin(r.gstin());
        users.save(u);
        notifications.notifyAdmins("New seller registration", r.storeName() + " is waiting for approval.");
        return Map.of("message", "Registration received. You can log in once an administrator approves your store.");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq r) {
        User u = users.findByEmail(r.email().trim().toLowerCase()).filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        switch (u.getStatus()) {
            case PENDING -> throw ApiException.forbidden("Your seller account is awaiting admin approval");
            case REJECTED -> throw ApiException.forbidden("Your seller registration was rejected");
            case BLOCKED -> throw ApiException.forbidden("Your account has been blocked. Contact support.");
            default -> {}
        }
        return Map.of("token", jwt.generate(u), "user", u);
    }

    @GetMapping("/me") public User me() { return Auth.require(); }

    private User create(String name, String email, String pw, String phone, Role role, UserStatus status) {
        String e = email.trim().toLowerCase();
        if (users.existsByEmail(e)) throw ApiException.bad("An account with this email already exists");
        User u = new User(); u.setName(name.trim()); u.setEmail(e); u.setPassword(encoder.encode(pw)); u.setPhone(phone); u.setRole(role); u.setStatus(status);
        return users.save(u);
    }
}
