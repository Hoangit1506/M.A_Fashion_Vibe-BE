package com.ma_fashion_vibe_be.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    String ward;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống")
    String street;

    String label; // VD: Nhà riêng, Công ty
    Boolean isDefault;
}