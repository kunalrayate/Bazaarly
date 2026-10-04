package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface OrderRepo extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Page<Order> findByStatus(OrderStatus status, Pageable p);
    boolean existsByUserIdAndStatusAndItemsProductId(Long userId, OrderStatus status, Long productId);
    long countByUserId(Long userId);

    @Query(value = "select distinct o from Order o join o.items i where i.product.seller.id = :sid and (:status is null or o.status = :status)",
           countQuery = "select count(distinct o) from Order o join o.items i where i.product.seller.id = :sid and (:status is null or o.status = :status)")
    Page<Order> findBySeller(@Param("sid") Long sid, @Param("status") OrderStatus status, Pageable p);

    @Query("select count(i) > 0 from OrderItem i where i.order.id = :oid and i.product.seller.id = :sid")
    boolean sellerOwns(@Param("oid") Long orderId, @Param("sid") Long sellerId);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> statusCounts();
    @Query("select coalesce(sum(o.total), 0) from Order o where o.status not in :excl")
    BigDecimal revenue(@Param("excl") Collection<OrderStatus> excl);
    long countByStatusNotIn(Collection<OrderStatus> excl);
    List<Order> findByCreatedAtAfterAndStatusNotIn(LocalDateTime since, Collection<OrderStatus> excl);
}
