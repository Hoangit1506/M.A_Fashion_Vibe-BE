package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.order.OrderRequest;
import com.ma_fashion_vibe_be.dto.order.OrderResponse;
import com.ma_fashion_vibe_be.dto.order.OrderStatusUpdateRequest;
import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.enums.PaymentMethod;
import com.ma_fashion_vibe_be.enums.PaymentStatus;
import com.ma_fashion_vibe_be.service.OrderService;
import com.ma_fashion_vibe_be.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {
    OrderService orderService;
    VNPayService vnPayService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request, HttpServletRequest httpServletRequest) {
        // 1. Tạo đơn hàng và trừ kho như bình thường
        OrderResponse newOrder = orderService.placeOrder(request);

        // 2. KIỂM TRA NẾU LÀ VNPAY THÌ TẠO LINK THANH TOÁN
        if (newOrder.getPaymentMethod() == PaymentMethod.VNPAY) {
            // Tổng tiền = Tiền hàng + Phí ship - Giảm giá
            long finalAmountToPay = newOrder.getTotalAmount()
                    .add(newOrder.getShippingFee())
                    .subtract(newOrder.getDiscount())
                    .longValue();

            // Tạo link và gán vào Response
            String paymentUrl = vnPayService.createOrderUrl(newOrder.getOrderNumber(), finalAmountToPay, httpServletRequest);
            newOrder.setPaymentUrl(paymentUrl);
        }

        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Tạo đơn hàng thành công")
                .result(newOrder)
                .build();
    }

    @GetMapping("/payment/vnpay-callback")
    public ApiResponse<String> vnpayCallback(@RequestParam Map<String, String> queryParams) {
        // 1. Kiểm tra chữ ký bảo mật xem có phải hacker can thiệp không
        boolean isValid = vnPayService.verifyPayment(queryParams);
        if (!isValid) {
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("Dữ liệu không hợp lệ hoặc bị giả mạo")
                    .build();
        }

        // 2. Lấy mã đơn và mã phản hồi từ VNPAY
        String orderNumber = queryParams.get("vnp_TxnRef");
        String responseCode = queryParams.get("vnp_ResponseCode");
        String vnpAmount = queryParams.get("vnp_Amount");

        // 3. Gọi Service để cập nhật trạng thái đơn và kho
        orderService.processVnpayCallback(orderNumber, responseCode, vnpAmount);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Cập nhật thanh toán thành công")
                .result(responseCode)
                .build();
    }


    // ĐÃ THÊM: Dành cho Admin lấy danh sách
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Object> getOrdersForAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var orderPage = orderService.getOrdersForAdmin(page, size, keyword, status, paymentStatus, startDate, endDate, sortBy, direction);
        return ApiResponse.builder()
                .success(true)
                .result(Map.of(
                        "data", orderPage.getContent(),
                        "currentPage", orderPage.getNumber() + 1,
                        "totalPages", orderPage.getTotalPages(),
                        "totalElements", orderPage.getTotalElements()
                ))
                .build();
    }

    // ĐÃ THÊM: Dành cho Admin đổi trạng thái đơn
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        orderService.updateOrderStatus(id, request.getNewStatus(), adminUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật trạng thái đơn hàng thành công")
                .build();
    }

    // ĐÃ THÊM: Dành cho Admin xem chi tiết 1 đơn hàng
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Object> getAdminOrderDetail(@PathVariable Long id) {
        return ApiResponse.builder()
                .success(true)
                .result(orderService.getOrderDetailForAdmin(id))
                .build();
    }

    // ĐÃ THÊM: Lấy danh sách đơn của tôi
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Object> getMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        var orderPage = orderService.getMyOrders(page, size, keyword, status, paymentStatus, startDate, endDate);
        return ApiResponse.builder()
                .success(true)
                .result(Map.of("data", orderPage.getContent(), "currentPage", orderPage.getNumber() + 1, "totalPages", orderPage.getTotalPages(), "totalElements", orderPage.getTotalElements()))
                .build();
    }

    // ĐÃ THÊM: API Khách hàng xem chi tiết đơn
    @GetMapping("/me/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Object> getMyOrderDetail(@PathVariable Long id) {
        return ApiResponse.builder()
                .success(true)
                .result(orderService.getMyOrderDetail(id))
                .build();
    }

    // ĐÃ THÊM: Khách hàng tự hủy đơn
    @PutMapping("/me/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> cancelMyOrder(@PathVariable Long id) {
        orderService.cancelMyOrder(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã hủy đơn hàng thành công")
                .build();
    }

    // ĐÃ THÊM: Khách hàng bấm Mua lại
    @PostMapping("/me/{id}/repurchase")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
    public ApiResponse<Void> repurchaseOrder(@PathVariable Long id) {
        orderService.repurchaseOrder(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã thêm các sản phẩm vào giỏ hàng")
                .build();
    }
}