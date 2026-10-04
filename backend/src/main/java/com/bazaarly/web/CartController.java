package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.repo.*;
import com.bazaarly.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class CartController {
    private final CartRepo cart;
    private final ProductRepo products;
    private final WishlistRepo wishlist;
    private final PricingService pricing;

    public record AddReq(Long productId, List<Long> variantIds, Integer quantity) {}
    public record QtyReq(int quantity) {}

    @GetMapping("/cart")
    public PricingService.Summary view(@RequestParam(required = false) String coupon) {
        return pricing.summarize(cart.findByUserIdOrderByIdAsc(Auth.require().getId()), coupon);
    }

    @GetMapping("/cart/count")
    public Map<String, Integer> count() { return Map.of("count", cart.findByUserIdOrderByIdAsc(Auth.require().getId()).stream().mapToInt(CartItem::getQuantity).sum()); }

    @PostMapping("/cart") @Transactional
    public Map<String, String> add(@RequestBody AddReq r) {
        User u = Auth.require();
        Product p = products.findById(r.productId()).filter(Product::isActive).orElseThrow(() -> ApiException.notFound("Product not available"));
        int qty = r.quantity() == null ? 1 : r.quantity();
        if (qty < 1) throw ApiException.bad("Quantity must be at least 1");
        List<Long> ids = r.variantIds() == null ? List.of() : r.variantIds();
        Set<String> chosenTypes = new HashSet<>();
        for (Long vid : ids) {
            ProductVariant v = p.getVariants().stream().filter(x -> x.getId().equals(vid)).findFirst().orElseThrow(() -> ApiException.bad("Invalid option selected"));
            chosenTypes.add(v.getType());
        }
        for (String type : p.getVariants().stream().map(ProductVariant::getType).collect(Collectors.toCollection(LinkedHashSet::new)))
            if (!chosenTypes.contains(type)) throw ApiException.bad("Please select " + type);
        String key = ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        CartItem ci = cart.findByUserIdAndProductIdAndVariantIds(u.getId(), p.getId(), key).orElseGet(() -> { CartItem n = new CartItem(); n.setUser(u); n.setProduct(p); n.setVariantIds(key); n.setQuantity(0); return n; });
        int total = ci.getQuantity() + qty;
        if (total > p.getStock()) throw ApiException.bad(p.getStock() == 0 ? "Out of stock" : "Only " + p.getStock() + " units available");
        if (total > 10) throw ApiException.bad("You can buy at most 10 units of a product per order");
        ci.setQuantity(total); cart.save(ci);
        return Map.of("message", "Added to cart");
    }

    @PutMapping("/cart/{id}")
    public Map<String, String> setQty(@PathVariable Long id, @RequestBody QtyReq r) {
        CartItem ci = cart.findByIdAndUserId(id, Auth.require().getId()).orElseThrow(() -> ApiException.notFound("Cart item not found"));
        if (r.quantity() < 1) throw ApiException.bad("Quantity must be at least 1");
        if (r.quantity() > ci.getProduct().getStock()) throw ApiException.bad("Only " + ci.getProduct().getStock() + " units available");
        if (r.quantity() > 10) throw ApiException.bad("You can buy at most 10 units of a product per order");
        ci.setQuantity(r.quantity()); cart.save(ci); return Map.of("message", "Updated");
    }

    @DeleteMapping("/cart/{id}")
    public Map<String, String> remove(@PathVariable Long id) {
        cart.findByIdAndUserId(id, Auth.require().getId()).ifPresent(cart::delete); return Map.of("message", "Removed");
    }

    @PostMapping("/cart/{id}/move-to-wishlist") @Transactional
    public Map<String, String> toWishlist(@PathVariable Long id) {
        User u = Auth.require();
        CartItem ci = cart.findByIdAndUserId(id, u.getId()).orElseThrow(() -> ApiException.notFound("Cart item not found"));
        if (wishlist.findByUserIdAndProductId(u.getId(), ci.getProduct().getId()).isEmpty()) { WishlistItem w = new WishlistItem(); w.setUser(u); w.setProduct(ci.getProduct()); wishlist.save(w); }
        cart.delete(ci); return Map.of("message", "Saved for later");
    }
}
