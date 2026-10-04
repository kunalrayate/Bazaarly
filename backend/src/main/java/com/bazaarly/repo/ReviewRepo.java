package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface ReviewRepo extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndHiddenFalseOrderByCreatedAtDesc(Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    List<Review> findByReportedTrueAndHiddenFalseOrderByCreatedAtDesc();
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
