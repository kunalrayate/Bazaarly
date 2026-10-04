package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import com.bazaarly.repo.ReviewRepo;
import com.bazaarly.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;
    private final ReviewRepo reviews;

    public record ReviewReq(int rating, String title, String comment) {}
    public record ReportReq(String reason) {}

    @GetMapping("/products/{id}/reviews") public Map<String, Object> list(@PathVariable Long id) { return service.summary(id); }

    @GetMapping("/products/{id}/can-review") public Map<String, Boolean> can(@PathVariable Long id) { return Map.of("canReview", service.canReview(Auth.current(), id)); }

    @PostMapping("/products/{id}/reviews")
    public Review add(@PathVariable Long id, @RequestBody ReviewReq r) { return service.create(Auth.require(), id, r.rating(), r.title(), r.comment()); }

    @PostMapping("/reviews/{id}/report")
    public Map<String, String> report(@PathVariable Long id, @RequestBody ReportReq r) {
        Auth.require(); Review rv = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Review not found"));
        rv.setReported(true); rv.setReportReason(r.reason()); reviews.save(rv);
        return Map.of("message", "Thanks, our team will review this.");
    }
}
