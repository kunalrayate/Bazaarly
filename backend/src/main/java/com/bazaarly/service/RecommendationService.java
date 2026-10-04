package com.bazaarly.service;

import com.bazaarly.entity.*;
import com.bazaarly.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.*;

/** Simple content + behaviour based recommendations: views, purchases, wishlist, category, co-purchases. */
@Service @RequiredArgsConstructor
public class RecommendationService {
    private final ProductRepo products;
    private final RecentViewRepo views;
    private final WishlistRepo wishlist;
    private final OrderItemRepo orderItems;

    public List<Product> forUser(User u, int limit) {
        LinkedHashMap<Long, Product> result = new LinkedHashMap<>();
        if (u != null) {
            Set<Long> cats = new LinkedHashSet<>();
            views.findTop20ByUserIdOrderByViewedAtDesc(u.getId()).forEach(v -> addCat(cats, v.getProduct()));
            wishlist.findByUserIdOrderByIdDesc(u.getId()).forEach(w -> addCat(cats, w.getProduct()));
            List<Long> bought = orderItems.purchasedProductIds(u.getId());
            products.findAllById(bought).forEach(p -> addCat(cats, p));
            if (!cats.isEmpty())
                products.findByActiveTrueAndCategoryIdInOrderByRatingAvgDescSoldCountDesc(cats, PageRequest.of(0, limit * 2))
                        .stream().filter(p -> !bought.contains(p.getId()) && p.getStock() > 0).limit(limit).forEach(p -> result.put(p.getId(), p));
            // "customers also bought" for the last purchase
            if (!bought.isEmpty() && result.size() < limit)
                products.findAllById(orderItems.frequentlyBoughtWith(bought.get(bought.size() - 1), PageRequest.of(0, 6)))
                        .stream().filter(p -> !bought.contains(p.getId())).forEach(p -> result.putIfAbsent(p.getId(), p));
        }
        if (result.size() < limit) products.findByActiveTrueOrderBySoldCountDesc(PageRequest.of(0, limit * 2)).forEach(p -> { if (result.size() < limit) result.putIfAbsent(p.getId(), p); });
        return new ArrayList<>(result.values());
    }

    private void addCat(Set<Long> cats, Product p) { if (p != null && p.getCategory() != null) cats.add(p.getCategory().getId()); }

    public List<Product> similar(Product p, int limit) {
        if (p.getCategory() == null) return List.of();
        return products.findByActiveTrueAndCategoryIdAndIdNotOrderByRatingAvgDesc(p.getCategory().getId(), p.getId(), PageRequest.of(0, limit));
    }

    public List<Product> frequentlyBought(Long productId, int limit) {
        List<Long> ids = orderItems.frequentlyBoughtWith(productId, PageRequest.of(0, limit));
        Map<Long, Product> m = new HashMap<>(); products.findAllById(ids).forEach(p -> m.put(p.getId(), p));
        return ids.stream().map(m::get).filter(Objects::nonNull).toList();
    }
}
