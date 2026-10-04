package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Profile, addresses, wishlist and notifications for the logged-in user. */
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AccountController {
    private final UserRepo users;
    private final AddressRepo addresses;
    private final WishlistRepo wishlist;
    private final ProductRepo products;
    private final NotificationRepo notifications;
    private final PasswordEncoder encoder;

    public record ProfileReq(String name, String phone, String storeName, String storeDescription, String businessAddress) {}
    public record PasswordReq(String currentPassword, String newPassword) {}

    // ---- profile
    @GetMapping("/me") public User me() { return Auth.require(); }

    @PutMapping("/me")
    public User update(@RequestBody ProfileReq r) {
        User u = Auth.require();
        if (r.name() != null && !r.name().isBlank()) u.setName(r.name().trim());
        u.setPhone(r.phone());
        if (u.getRole() == Enums.Role.SELLER) { if (r.storeName() != null && !r.storeName().isBlank()) u.setStoreName(r.storeName()); u.setStoreDescription(r.storeDescription()); u.setBusinessAddress(r.businessAddress()); }
        return users.save(u);
    }

    @PutMapping("/me/password")
    public Map<String, String> password(@RequestBody PasswordReq r) {
        User u = Auth.require();
        if (!encoder.matches(r.currentPassword(), u.getPassword())) throw ApiException.bad("Current password is incorrect");
        if (r.newPassword() == null || r.newPassword().length() < 6) throw ApiException.bad("New password must be at least 6 characters");
        u.setPassword(encoder.encode(r.newPassword())); users.save(u); return Map.of("message", "Password updated");
    }

    // ---- addresses
    @GetMapping("/addresses") public List<Address> addresses() { return addresses.findByUserIdOrderByDefaultAddressDescIdDesc(Auth.require().getId()); }

    @PostMapping("/addresses") @Transactional
    public Address add(@RequestBody Address a) {
        User u = Auth.require(); a.setId(null); a.setUser(u); validate(a);
        boolean first = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId()).isEmpty();
        if (first) a.setDefaultAddress(true);
        if (a.isDefaultAddress()) clearDefault(u);
        return addresses.save(a);
    }

    @PutMapping("/addresses/{id}") @Transactional
    public Address edit(@PathVariable Long id, @RequestBody Address in) {
        User u = Auth.require(); Address a = addresses.findByIdAndUserId(id, u.getId()).orElseThrow(() -> ApiException.notFound("Address not found"));
        a.setFullName(in.getFullName()); a.setPhone(in.getPhone()); a.setLine1(in.getLine1()); a.setLine2(in.getLine2());
        a.setCity(in.getCity()); a.setState(in.getState()); a.setPincode(in.getPincode()); validate(a);
        if (in.isDefaultAddress()) { clearDefault(u); a.setDefaultAddress(true); }
        return addresses.save(a);
    }

    @DeleteMapping("/addresses/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        addresses.findByIdAndUserId(id, Auth.require().getId()).ifPresent(addresses::delete); return Map.of("message", "Deleted");
    }

    private void clearDefault(User u) { addresses.findByUserIdOrderByDefaultAddressDescIdDesc(u.getId()).forEach(x -> { x.setDefaultAddress(false); addresses.save(x); }); }
    private void validate(Address a) {
        if (blank(a.getFullName()) || blank(a.getPhone()) || blank(a.getLine1()) || blank(a.getCity()) || blank(a.getState()) || blank(a.getPincode())) throw ApiException.bad("Please fill all required address fields");
        if (!a.getPincode().matches("\\d{6}")) throw ApiException.bad("Pincode must be 6 digits");
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }

    // ---- wishlist
    @GetMapping("/wishlist") public List<Product> wishlist() { return wishlist.findByUserIdOrderByIdDesc(Auth.require().getId()).stream().map(WishlistItem::getProduct).toList(); }
    @GetMapping("/wishlist/ids") public List<Long> wishlistIds() { return wishlist.findByUserIdOrderByIdDesc(Auth.require().getId()).stream().map(w -> w.getProduct().getId()).toList(); }

    @PostMapping("/wishlist/{productId}")
    public Map<String, String> addWish(@PathVariable Long productId) {
        User u = Auth.require();
        if (wishlist.findByUserIdAndProductId(u.getId(), productId).isEmpty()) {
            WishlistItem w = new WishlistItem(); w.setUser(u); w.setProduct(products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"))); wishlist.save(w);
        }
        return Map.of("message", "Added to wishlist");
    }

    @DeleteMapping("/wishlist/{productId}")
    public Map<String, String> removeWish(@PathVariable Long productId) {
        wishlist.findByUserIdAndProductId(Auth.require().getId(), productId).ifPresent(wishlist::delete); return Map.of("message", "Removed from wishlist");
    }

    // ---- notifications
    @GetMapping("/notifications") public List<Notification> notifications() { return notifications.findTop50ByUserIdOrderByCreatedAtDesc(Auth.require().getId()); }
    @GetMapping("/notifications/unread-count") public Map<String, Long> unread() { return Map.of("count", notifications.countByUserIdAndReadFalse(Auth.require().getId())); }
    @PostMapping("/notifications/read-all")
    public Map<String, String> readAll() { var list = notifications.findByUserIdAndReadFalse(Auth.require().getId()); list.forEach(n -> n.setRead(true)); notifications.saveAll(list); return Map.of("message", "ok"); }
}
