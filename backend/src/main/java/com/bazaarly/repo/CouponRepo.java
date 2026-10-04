package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface CouponRepo extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    @Query("select c from Coupon c where c.active = true and (c.expiresAt is null or c.expiresAt > :now) and (c.startsAt is null or c.startsAt <= :now)")
    List<Coupon> findLive(@Param("now") LocalDateTime now);
}
