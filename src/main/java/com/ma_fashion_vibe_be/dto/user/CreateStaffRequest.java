package com.ma_fashion_vibe_be.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateStaffRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ!")
    String email;

    @NotBlank(message = "Họ tên không được để trống")
    String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    String confirmPassword;
}