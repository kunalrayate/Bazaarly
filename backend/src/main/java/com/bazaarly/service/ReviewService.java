package com.bazaarly.service;

import com.bazaarly.config.ApiException;
import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.OrderStatus;
import com.bazaarly.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepo reviews;
    private final ProductRepo products;
    private final OrderRepo orders;

    public boolean canReview(User u, Long productId) {
        return u != null && orders.existsByUserIdAndStatusAndItemsProductId(u.getId(), OrderStatus.DELIVERED, productId) && !reviews.existsByUserIdAndProductId(u.getId(), productId);
    }

    @Transactional
    public Review create(User u, Long productId, int rating, String title, String comment) {
        if (rating < 1 || rating > 5) throw ApiException.bad("Rating must be between 1 and 5");
        Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (reviews.existsByUserIdAndProductId(u.getId(), productId)) throw ApiException.bad("You have already reviewed this product");
        if (!orders.existsByUserIdAndStatusAndItemsProductId(u.getId(), OrderStatus.DELIVERED, productId)) throw ApiException.bad("Only customers who received this product can review it");
        Review r = new Review(); r.setUser(u); r.setProduct(p); r.setRating(rating); r.setTitle(title); r.setComment(comment);
        reviews.save(r); recalc(p); return r;
    }

    @Transactional
    public void recalc(Product p) {
        List<Review> list = reviews.findByProductIdAndHiddenFalseOrderByCreatedAtDesc(p.getId());
        p.setRatingCount(list.size());
        p.setRatingAvg(list.isEmpty() ? 0 : Math.round(list.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0);
        products.save(p);
    }

    public Map<String, Object> summary(Long productId) {
        List<Review> list = reviews.findByProductIdAndHiddenFalseOrderByCreatedAtDesc(productId);
        Map<Integer, Long> dist = new LinkedHashMap<>(); for (int i = 5; i >= 1; i--) dist.put(i, 0L);
        list.forEach(r -> dist.merge(r.getRating(), 1L, Long::sum));
        double avg = list.stream().mapToInt(Review::getRating).average().orElse(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("average", Math.round(avg * 10) / 10.0); m.put("count", list.size()); m.put("distribution", dist); m.put("reviews", list);
        return m;
    }
}
