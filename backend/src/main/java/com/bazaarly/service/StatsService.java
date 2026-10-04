package com.bazaarly.service;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class StatsService {
    private static final List<OrderStatus> EXCL = List.of(OrderStatus.CANCELLED, OrderStatus.RETURNED);
    private final OrderRepo orders;
    private final OrderItemRepo items;
    private final ProductRepo products;
    private final UserRepo users;

    private List<Map<String, Object>> trend(Map<LocalDate, BigDecimal> rev, Map<LocalDate, Integer> cnt, int days) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            out.add(Map.of("date", d.toString(), "revenue", rev.getOrDefault(d, BigDecimal.ZERO), "orders", cnt.getOrDefault(d, 0)));
        }
        return out;
    }
    private Map<String, Long> statusMap(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>(); for (OrderStatus s : OrderStatus.values()) m.put(s.name(), 0L);
        rows.forEach(r -> m.put(String.valueOf(r[0]), ((Number) r[1]).longValue())); return m;
    }
    private List<Map<String, Object>> top(List<Product> ps) {
        return ps.stream().map(p -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.getId()); m.put("name", p.getName()); m.put("sold", p.getSoldCount());
            m.put("stock", p.getStock()); m.put("image", p.getImages().isEmpty() ? null : p.getImages().get(0)); m.put("revenue", p.getSellingPrice().multiply(BigDecimal.valueOf(p.getSoldCount()))); return m; }).toList();
    }

    public Map<String, Object> seller(User s) {
        Long sid = s.getId(); Map<String, Object> m = new LinkedHashMap<>();
        m.put("revenue", items.sellerRevenue(sid, EXCL)); m.put("unitsSold", items.sellerUnits(sid, EXCL));
        m.put("totalOrders", items.sellerOrderCount(sid)); m.put("products", products.countBySellerId(sid));
        m.put("statusCounts", statusMap(items.sellerStatusCounts(sid)));
        Map<LocalDate, BigDecimal> rev = new HashMap<>(); Map<LocalDate, Integer> cnt = new HashMap<>();
        items.sellerItemsSince(sid, LocalDate.now().minusDays(13).atStartOfDay(), EXCL).forEach(i -> {
            LocalDate d = i.getOrder().getCreatedAt().toLocalDate(); rev.merge(d, i.lineTotal(), BigDecimal::add); cnt.merge(d, i.getQuantity(), Integer::sum); });
        m.put("salesTrend", trend(rev, cnt, 14));
        m.put("bestSellers", top(products.findBySellerIdAndActiveTrueOrderBySoldCountDesc(sid, PageRequest.of(0, 5))));
        m.put("lowStock", top(products.lowStockBySeller(sid, PageRequest.of(0, 10))));
        return m;
    }

    public Map<String, Object> admin() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revenue", orders.revenue(EXCL)); m.put("totalOrders", orders.count()); m.put("customers", users.countByRole(Role.CUSTOMER));
        m.put("sellers", users.countByRoleAndStatus(Role.SELLER, UserStatus.ACTIVE)); m.put("pendingSellers", users.countByRoleAndStatus(Role.SELLER, UserStatus.PENDING));
        m.put("products", products.count()); m.put("statusCounts", statusMap(orders.statusCounts()));
        Map<LocalDate, BigDecimal> rev = new HashMap<>(); Map<LocalDate, Integer> cnt = new HashMap<>();
        orders.findByCreatedAtAfterAndStatusNotIn(LocalDate.now().minusDays(13).atStartOfDay(), EXCL).forEach(o -> {
            LocalDate d = o.getCreatedAt().toLocalDate(); rev.merge(d, o.getTotal(), BigDecimal::add); cnt.merge(d, 1, Integer::sum); });
        m.put("salesTrend", trend(rev, cnt, 14));
        m.put("popularProducts", top(products.findByActiveTrueOrderBySoldCountDesc(PageRequest.of(0, 5))));
        List<Map<String, Object>> cats = new ArrayList<>();
        items.popularCategories(EXCL, PageRequest.of(0, 6)).forEach(r -> cats.add(Map.of("name", r[0], "units", ((Number) r[1]).longValue())));
        m.put("popularCategories", cats);
        m.put("lowStock", top(products.lowStock(PageRequest.of(0, 10))));
        return m;
    }
}
