package com.ma_fashion_vibe_be.dto.dashboard;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChartDataResponse {
    String date;           // Ngày (VD: 01/04)
    BigDecimal revenue;    // Doanh thu trong ngày đó
    Long orderCount;       // Số đơn hàng trong ngày đó
}