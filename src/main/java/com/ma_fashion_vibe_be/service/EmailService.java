package com.ma_fashion_vibe_be.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailService {

    final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    String fromEmail;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Mã xác thực đăng ký tài khoản M.A Fashion Vibe");
        message.setText("Xin chào,\n\n" +
                "Bạn vừa đăng ký tài khoản tại hệ thống M.A Fashion Vibe.\n" +
                "Mã xác thực (OTP) của bạn là: " + otp + "\n\n" +
                "Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n" +
                "Trân trọng,\nĐội ngũ M.A Fashion Vibe");

        mailSender.send(message);
    }

    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Chào mừng bạn đến với M.A Fashion Vibe!");
        message.setText("Xin chào " + name + ",\n\n" +
                "Chúc mừng bạn đã đăng ký thành công tài khoản tại M.A Fashion Vibe.\n" +
                "Tài khoản của bạn đã được kích hoạt. Hãy bắt đầu khám phá và định hình phong cách thời trang của riêng bạn ngay hôm nay!\n\n" +
                "Trân trọng,\nĐội ngũ M.A Fashion Vibe");

        mailSender.send(message);
    }

    public void sendForgotPasswordEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Yêu cầu khôi phục mật khẩu M.A Fashion Vibe");
        message.setText("Xin chào,\n\n" +
                "Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn.\n" +
                "Mã xác thực (OTP) của bạn là: " + otp + "\n\n" +
                "Mã này có hiệu lực trong vòng 5 phút. Nếu bạn không yêu cầu thay đổi mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\nĐội ngũ M.A Fashion Vibe");

        mailSender.send(message);
    }
}