package com.ma_fashion_vibe_be.controller;

import com.ma_fashion_vibe_be.dto.ApiResponse;
import com.ma_fashion_vibe_be.dto.auth.*;
import com.ma_fashion_vibe_be.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/send-register-otp")
    public ApiResponse<Void> sendRegisterOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendRegisterOtp(request.getEmail());
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã gửi mã xác thực (OTP) đến email của bạn!")
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đăng ký tài khoản thành công!")
                .build();
    }

    @PostMapping("/send-forgot-password-otp")
    public ApiResponse<Void> sendForgotPasswordOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendForgotPasswordOtp(request.getEmail());
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã gửi mã xác thực (OTP) khôi phục mật khẩu đến email của bạn!")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest){

        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .result(authService.login(request, httpServletRequest))
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@RequestBody java.util.Map<String, String> requestBody, HttpServletRequest httpServletRequest) {
        String idToken = requestBody.get("idToken");
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Đăng nhập Google thành công")
                .result(authService.googleLogin(idToken, httpServletRequest))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody RefreshTokenRequest request){

        authService.logout(request.getRefreshToken());

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Logout success")
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {

        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .result(authService.refreshToken(request, httpServletRequest))
                .build();
    }
}