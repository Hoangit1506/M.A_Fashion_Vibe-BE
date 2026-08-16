package com.ma_fashion_vibe_be.dto.inventory;

import com.ma_fashion_vibe_be.enums.StockChangeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustmentRequest {
    @NotNull(message = "Thiếu ID Phân loại hàng")
    Long variantId;

    @NotNull(message = "Số lượng thay đổi không được để trống")
    Integer changeQuantity; // Ví dụ: Nhập 100 thì truyền 100. Xuất hỏng 5 cái thì truyền -5.

    @NotNull(message = "Thiếu loại thay đổi")
    StockChangeType changeType; // IMPORT, ADJUSTMENT, RETURN...

    String note; // Ghi chú (VD: Lô hàng bị rách do vận chuyển)
}