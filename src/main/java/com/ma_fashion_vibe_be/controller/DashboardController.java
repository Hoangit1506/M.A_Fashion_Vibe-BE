package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.service.DashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được xem thống kê
public class DashboardController {

    DashboardService dashboardService;

    @GetMapping("/stats")
    public ApiResponse<Object> getFullDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Instant start = null;
        Instant end = null;

        // Xử lý parse ngày từ Frontend (Ví dụ: 2024-04-01)
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate).atTime(23, 59, 59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            }
        } catch (Exception e) {
            // Nếu parse lỗi thì để null, Repository sẽ tự hiểu là lấy tất cả
        }

        return ApiResponse.builder()
                .success(true)
                .result(Map.of(
                        "kpi", dashboardService.getGeneralStats(start, end),
                        "charts", dashboardService.getChartData(start, end),
                        "orderStatus", dashboardService.getOrderStatusStats(start, end),
                        "reports", dashboardService.getTopAndAlerts()
                ))
                .build();
    }
}