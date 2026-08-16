package com.ma_fashion_vibe_be.dto.order;

import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.enums.PaymentMethod;
import com.ma_fashion_vibe_be.enums.PaymentStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderAdminResponse {
    Long id;
    String orderNumber;
    String receiverName; // Trích từ Address để hiển thị nhanh
    String phone;        // Trích từ Address
    BigDecimal totalAmount;
    OrderStatus status;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    Instant createdAt;
    Instant updatedAt;
}