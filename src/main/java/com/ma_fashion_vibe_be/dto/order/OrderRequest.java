package com.ma_fashion_vibe_be.dto.order;

import com.ma_fashion_vibe_be.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderRequest {

    @NotNull(message = "Vui lòng chọn địa chỉ giao hàng")
    Long addressId;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    PaymentMethod paymentMethod;

    String note;
}