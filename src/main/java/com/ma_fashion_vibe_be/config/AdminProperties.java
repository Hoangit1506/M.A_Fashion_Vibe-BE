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
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {
    String email;
    String password;
    String name;
}
