package com.bazaarly.service;

import com.bazaarly.repo.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/** Ends limited-time deals once their end date has passed. */
@Component @RequiredArgsConstructor
public class ProductScheduler {
    private final ProductRepo products;

    @Scheduled(fixedRate = 60_000)
    public void expireDeals() {
        products.expiredDeals(LocalDateTime.now()).forEach(p -> { p.setDiscountPercent(0); p.setDiscountEndsAt(null); p.computePrice(); products.save(p); });
    }
}
