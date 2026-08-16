package com.ma_fashion_vibe_be.dto.dashboard;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardGeneralStats {
    BigDecimal totalRevenue;        // Tổng doanh thu
    Long totalOrders;               // Tổng số đơn hàng
    Long totalProductsSold;         // Tổng số sản phẩm đã bán
    Long totalBuyingCustomers;      // Số khách hàng thực tế đã mua
    Long totalNewCustomers;         // Số lượng tài khoản mới tạo
}