package com.ma_fashion_vibe_be.config;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Component
@ConfigurationProperties(prefix = "app.vnpay")
public class VnpayProperties {
    String tmnCode;
    String hashSecret;
    String url;
    String returnUrl;
}