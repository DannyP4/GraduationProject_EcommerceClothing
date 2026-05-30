package com.uniform.store.repository;

import com.uniform.store.dto.response.OrdersByStatusDto;
import com.uniform.store.dto.response.PaymentBreakdownDto;
import com.uniform.store.dto.response.RevenueBucketDto;
import com.uniform.store.dto.response.TopCustomerDto;
import com.uniform.store.dto.response.TopProductDto;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.StatsGranularity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private static final String REVENUE_IN = "('PAID','PROCESSING','SHIPPED','DELIVERED')";
    private static final String CONVERT_LOCAL = "CONVERT_TZ(placed_at,'+00:00','+07:00')";

    private final NamedParameterJdbcTemplate jdbc;

    public RevenueTotals revenueTotals(Instant from, Instant to) {
        String sql = "SELECT COALESCE(SUM(grand_total), 0) AS revenue, COUNT(*) AS orders " +
                "FROM orders WHERE status IN " + REVENUE_IN +
                " AND placed_at >= :from AND placed_at < :to";
        return jdbc.queryForObject(sql, params(from, to),
                (rs, i) -> new RevenueTotals(
                        rs.getBigDecimal("revenue"),
                        rs.getLong("orders")));
    }

    public long countNewCustomers(Instant from, Instant to) {
        String sql = "SELECT COUNT(*) FROM users u " +
                "JOIN roles r ON r.id = u.role_id " +
                "WHERE r.name = 'customer' " +
                "AND u.created_at >= :from AND u.created_at < :to";
        Long n = jdbc.queryForObject(sql, params(from, to), Long.class);
        return n == null ? 0L : n;
    }

    public List<RevenueBucketDto> revenueTimeSeries(Instant from, Instant to, StatsGranularity granularity) {
        String bucketExpr = switch (granularity) {
            case DAY -> "DATE_FORMAT(" + CONVERT_LOCAL + ", '%Y-%m-%d')";
            case WEEK -> "DATE_FORMAT(" + CONVERT_LOCAL + ", '%x-W%v')";
            case MONTH -> "DATE_FORMAT(" + CONVERT_LOCAL + ", '%Y-%m')";
        };
        String sql = "SELECT " + bucketExpr + " AS bucket, " +
                "SUM(grand_total) AS revenue, COUNT(*) AS order_count " +
                "FROM orders WHERE status IN " + REVENUE_IN +
                " AND placed_at >= :from AND placed_at < :to " +
                "GROUP BY bucket ORDER BY bucket";
        return jdbc.query(sql, params(from, to),
                (rs, i) -> RevenueBucketDto.builder()
                        .bucket(rs.getString("bucket"))
                        .revenue(rs.getBigDecimal("revenue"))
                        .orderCount(rs.getLong("order_count"))
                        .build());
    }

    public List<PaymentBreakdownDto> paymentBreakdown(Instant from, Instant to) {
        String sql = "SELECT p.provider, SUM(o.grand_total) AS revenue, " +
                "COUNT(DISTINCT o.id) AS order_count " +
                "FROM orders o " +
                "JOIN payments p ON p.order_id = o.id AND p.status = 'CAPTURED' " +
                "WHERE o.status IN " + REVENUE_IN +
                " AND o.placed_at >= :from AND o.placed_at < :to " +
                "GROUP BY p.provider ORDER BY revenue DESC";
        List<PaymentBreakdownDto> rows = jdbc.query(sql, params(from, to),
                (rs, i) -> PaymentBreakdownDto.builder()
                        .provider(PaymentProvider.valueOf(rs.getString("provider")))
                        .revenue(rs.getBigDecimal("revenue"))
                        .orderCount(rs.getLong("order_count"))
                        .pct(0.0)
                        .build());
        BigDecimal total = rows.stream()
                .map(PaymentBreakdownDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) return rows;
        return rows.stream()
                .map(r -> PaymentBreakdownDto.builder()
                        .provider(r.getProvider())
                        .revenue(r.getRevenue())
                        .orderCount(r.getOrderCount())
                        .pct(r.getRevenue()
                                .divide(total, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue())
                        .build())
                .toList();
    }

    public List<OrdersByStatusDto> ordersByStatus(Instant from, Instant to) {
        String sql = "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(grand_total), 0) AS revenue " +
                "FROM orders WHERE placed_at >= :from AND placed_at < :to " +
                "GROUP BY status";
        return jdbc.query(sql, params(from, to),
                (rs, i) -> OrdersByStatusDto.builder()
                        .status(OrderStatus.valueOf(rs.getString("status")))
                        .count(rs.getLong("cnt"))
                        .revenue(rs.getBigDecimal("revenue"))
                        .build());
    }

    public List<TopProductDto> topProducts(Instant from, Instant to, int limit) {
        String sql = "SELECT v.product_id AS product_id, MAX(oi.product_name) AS product_name, " +
                "SUM(oi.quantity) AS units_sold, SUM(oi.line_total) AS revenue " +
                "FROM order_items oi " +
                "JOIN orders o ON o.id = oi.order_id " +
                "JOIN product_variants v ON v.id = oi.variant_id " +
                "WHERE o.status IN " + REVENUE_IN +
                " AND o.placed_at >= :from AND o.placed_at < :to " +
                "GROUP BY v.product_id ORDER BY revenue DESC LIMIT :lim";
        MapSqlParameterSource p = params(from, to).addValue("lim", limit);
        return jdbc.query(sql, p,
                (rs, i) -> TopProductDto.builder()
                        .productId(rs.getLong("product_id"))
                        .productName(rs.getString("product_name"))
                        .unitsSold(rs.getLong("units_sold"))
                        .revenue(rs.getBigDecimal("revenue"))
                        .build());
    }

    public List<TopCustomerDto> topCustomers(Instant from, Instant to, int limit) {
        String sql = "SELECT u.id AS user_id, u.email, u.full_name, " +
                "COUNT(o.id) AS order_count, SUM(o.grand_total) AS total_spent " +
                "FROM orders o JOIN users u ON u.id = o.user_id " +
                "WHERE o.status IN " + REVENUE_IN +
                " AND o.placed_at >= :from AND o.placed_at < :to " +
                "GROUP BY u.id, u.email, u.full_name " +
                "ORDER BY total_spent DESC LIMIT :lim";
        MapSqlParameterSource p = params(from, to).addValue("lim", limit);
        return jdbc.query(sql, p,
                (rs, i) -> TopCustomerDto.builder()
                        .userId(rs.getLong("user_id"))
                        .email(rs.getString("email"))
                        .fullName(rs.getString("full_name"))
                        .orderCount(rs.getLong("order_count"))
                        .totalSpent(rs.getBigDecimal("total_spent"))
                        .build());
    }

    private static MapSqlParameterSource params(Instant from, Instant to) {
        return new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
    }

    public record RevenueTotals(BigDecimal revenue, long orders) {}
}
