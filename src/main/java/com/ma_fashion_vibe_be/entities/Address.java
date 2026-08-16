package com.ma_fashion_vibe_be.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class Address {
    // For VN: province, district, ward; street contains house number + street name
    String receiverName;
    String phone;
    String province;   // tỉnh / thành phố
    String district;   // quận / huyện
    String ward;       // phường / xã

    @Column(columnDefinition = "LONGTEXT")
    String street;     // số nhà + tên đường / thôn / tổ

    @Column(columnDefinition = "LONGTEXT")
    String note;       // optional (giao hàng, hướng dẫn)
}
