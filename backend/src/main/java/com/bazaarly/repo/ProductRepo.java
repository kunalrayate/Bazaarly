package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface ProductRepo extends JpaRepository<Product, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> lockById(@Param("id") Long id);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
    @Query("select p from Product p where p.seller.id = :sid and lower(p.name) like lower(concat('%', :q, '%'))")
    Page<Product> searchBySeller(@Param("sid") Long sid, @Param("q") String q, Pageable pageable);
    long countBySellerId(Long sellerId);
    long countByActiveTrue();

    List<Product> findByActiveTrueAndFeaturedTrueOrderByIdDesc(Pageable p);
    List<Product> findByActiveTrueOrderByViewCountDesc(Pageable p);
    List<Product> findByActiveTrueOrderBySoldCountDesc(Pageable p);
    List<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable p);
    List<Product> findByActiveTrueAndDiscountPercentGreaterThanOrderByDiscountPercentDesc(int min, Pageable p);
    List<Product> findByActiveTrueAndCategoryIdInOrderByRatingAvgDescSoldCountDesc(Collection<Long> ids, Pageable p);
    List<Product> findByActiveTrueAndCategoryIdAndIdNotOrderByRatingAvgDesc(Long categoryId, Long id, Pageable p);

    @Query("select distinct p.brand from Product p where p.active = true and p.brand is not null and (:cat is null or p.category.id = :cat) order by p.brand")
    List<String> brands(@Param("cat") Long categoryId);

    @Query("select p from Product p where p.stock <= p.lowStockThreshold and p.active = true order by p.stock asc")
    List<Product> lowStock(Pageable p);
    @Query("select p from Product p where p.seller.id = :sid and p.stock <= p.lowStockThreshold and p.active = true order by p.stock asc")
    List<Product> lowStockBySeller(@Param("sid") Long sid, Pageable p);
    List<Product> findBySellerIdAndActiveTrueOrderBySoldCountDesc(Long sellerId, Pageable p);

    @Query("select p from Product p where p.discountEndsAt is not null and p.discountEndsAt < :now and p.discountPercent > 0")
    List<Product> expiredDeals(@Param("now") LocalDateTime now);
}
