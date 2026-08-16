package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.dashboard.*;
import com.ma_fashion_vibe_be.dto.product.ProductListResponse;
import com.ma_fashion_vibe_be.entities.Order;
import com.ma_fashion_vibe_be.entities.Product;
import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService {

    OrderRepository orderRepository;
    OrderItemRepository orderItemRepository;
    UserRepository userRepository;
    ProductRepository productRepository;
    InventoryRepository inventoryRepository;

    // 1. LẤY CHỈ SỐ KPI TỔNG QUAN
    public DashboardGeneralStats getGeneralStats(Instant start, Instant end) {
        BigDecimal revenue = orderRepository.calculateTotalRevenue(OrderStatus.DELIVERED, start, end);
        long orders = orderRepository.countTotalOrders(start, end);
        Long soldItems = orderItemRepository.sumProductsSold(OrderStatus.DELIVERED, start, end);
        long customers = orderRepository.countDistinctBuyingCustomers(OrderStatus.DELIVERED, start, end);
        long newUsers = userRepository.countNewCustomers(start, end);

        return DashboardGeneralStats.builder()
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalOrders(orders)
                .totalProductsSold(soldItems != null ? soldItems : 0L)
                .totalBuyingCustomers(customers)
                .totalNewCustomers(newUsers)
                .build();
    }

    // 2. LẤY DỮ LIỆU BIỂU ĐỒ (DOANH THU & ĐƠN HÀNG THEO NGÀY)
    public List<ChartDataResponse> getChartData(Instant start, Instant end) {
        List<Order> orders = orderRepository.findOrdersForChart(start, end);
        Map<String, ChartDataResponse> statsMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Gom nhóm dữ liệu theo ngày
        for (Order o : orders) {
            String dateStr = LocalDateTime.ofInstant(o.getCreatedAt(), ZoneId.of("Asia/Ho_Chi_Minh")).format(formatter);
            ChartDataResponse daily = statsMap.getOrDefault(dateStr, new ChartDataResponse(dateStr, BigDecimal.ZERO, 0L));

            if (o.getStatus() == OrderStatus.DELIVERED) {
                daily.setRevenue(daily.getRevenue().add(o.getTotalAmount()));
            }
            daily.setOrderCount(daily.getOrderCount() + 1);
            statsMap.put(dateStr, daily);
        }

        // Sắp xếp theo thứ tự thời gian và trả về
        return statsMap.values().stream()
                .sorted(Comparator.comparing(ChartDataResponse::getDate))
                .collect(Collectors.toList());
    }

    // 3. LẤY TỈ LỆ TRẠNG THÁI ĐƠN HÀNG (BIỂU ĐỒ TRÒN)
    public List<OrderStatusData> getOrderStatusStats(Instant start, Instant end) {
        List<Order> orders = orderRepository.findOrdersForChart(start, end);
        Map<OrderStatus, Long> counts = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        return Arrays.stream(OrderStatus.values())
                .map(status -> new OrderStatusData(status.name(), counts.getOrDefault(status, 0L)))
                .collect(Collectors.toList());
    }

    // 4. TOP 10 BÁN CHẠY & CẢNH BÁO KHO
    public Map<String, Object> getTopAndAlerts() {
        // Top 10 bán chạy
        // Chuyển đổi List<Product> thành List<Map> chỉ chứa ID, Name và SoldCount để tránh lỗi Đệ quy vô hạn JSON
        List<Map<String, Object>> topProducts = productRepository.findByActiveTrueOrderBySoldCountDesc(PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "soldCount", p.getSoldCount()
                )).collect(Collectors.toList());

        // Cảnh báo kho (Threshold = 10)
        var lowStockItems = inventoryRepository.findLowStockInventories(10).stream()
                .map(inv -> Map.of(
                        "productName", inv.getVariant().getProduct().getName(),
                        "sku", inv.getVariant().getSku(),
                        "color", inv.getVariant().getColor(),
                        "size", inv.getVariant().getSize(),
                        "available", inv.getQuantity() - inv.getReserved() - inv.getSafetyStock()
                )).collect(Collectors.toList());

        return Map.of(
                "topProducts", topProducts,
                "lowStockAlerts", lowStockItems
        );
    }
}