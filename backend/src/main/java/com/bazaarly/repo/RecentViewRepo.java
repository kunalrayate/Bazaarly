package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface RecentViewRepo extends JpaRepository<RecentView, Long> {
    List<RecentView> findTop20ByUserIdOrderByViewedAtDesc(Long userId);
    Optional<RecentView> findByUserIdAndProductId(Long userId, Long productId);
}
