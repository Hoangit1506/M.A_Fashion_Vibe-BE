package com.ma_fashion_vibe_be.dto.dashboard;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusData {
    String status;     // Tên trạng thái (PENDING, DELIVERED...)
    Long count;        // Số lượng đơn
}