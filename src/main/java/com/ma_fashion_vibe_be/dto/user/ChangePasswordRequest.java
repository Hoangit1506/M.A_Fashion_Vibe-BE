package com.ma_fashion_vibe_be.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    String oldPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    String confirmPassword;
}