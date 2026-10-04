package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {
    @Query("select coalesce(sum(i.unitPrice * i.quantity), 0) from OrderItem i where i.product.seller.id = :sid and i.order.status not in :excl")
    BigDecimal sellerRevenue(@Param("sid") Long sid, @Param("excl") Collection<OrderStatus> excl);
    @Query("select coalesce(sum(i.quantity), 0) from OrderItem i where i.product.seller.id = :sid and i.order.status not in :excl")
    Long sellerUnits(@Param("sid") Long sid, @Param("excl") Collection<OrderStatus> excl);
    @Query("select count(distinct i.order.id) from OrderItem i where i.product.seller.id = :sid")
    Long sellerOrderCount(@Param("sid") Long sid);
    @Query("select i.order.status, count(distinct i.order.id) from OrderItem i where i.product.seller.id = :sid group by i.order.status")
    List<Object[]> sellerStatusCounts(@Param("sid") Long sid);
    @Query("select i from OrderItem i where i.product.seller.id = :sid and i.order.createdAt > :since and i.order.status not in :excl")
    List<OrderItem> sellerItemsSince(@Param("sid") Long sid, @Param("since") LocalDateTime since, @Param("excl") Collection<OrderStatus> excl);
    @Query("select i.product.category.name, sum(i.quantity) from OrderItem i where i.order.status not in :excl group by i.product.category.name order by sum(i.quantity) desc")
    List<Object[]> popularCategories(@Param("excl") Collection<OrderStatus> excl, Pageable p);
    @Query("select i2.product.id from OrderItem i1, OrderItem i2 where i1.product.id = :pid and i2.order.id = i1.order.id and i2.product.id <> :pid group by i2.product.id order by count(i2) desc")
    List<Long> frequentlyBoughtWith(@Param("pid") Long pid, Pageable p);
    @Query("select distinct i.product.id from OrderItem i where i.order.user.id = :uid")
    List<Long> purchasedProductIds(@Param("uid") Long uid);
}
