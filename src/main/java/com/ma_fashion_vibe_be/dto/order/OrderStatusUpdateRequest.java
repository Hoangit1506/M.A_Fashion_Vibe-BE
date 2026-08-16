package com.ma_fashion_vibe_be.dto.order;

import com.ma_fashion_vibe_be.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "Trạng thái mới không được để trống")
    OrderStatus newStatus;
}