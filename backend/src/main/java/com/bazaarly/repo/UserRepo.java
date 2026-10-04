package com.bazaarly.repo;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByRoleAndStatus(Role role, UserStatus status);
    List<User> findByRole(Role role);
    @Query("select u from User u where u.role = :role and (:status is null or u.status = :status) and (:q = '' or lower(u.name) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')) or lower(coalesce(u.storeName,'')) like lower(concat('%', :q, '%')))")
    Page<User> search(@Param("role") Role role, @Param("status") UserStatus status, @Param("q") String q, Pageable pageable);
}
