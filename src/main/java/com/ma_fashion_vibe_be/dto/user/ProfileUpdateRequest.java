package com.ma_fashion_vibe_be.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    String fullName;

    String phone;

    // Ép định dạng ngày tháng chuẩn để React gửi xuống không bị lỗi
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dob;
}