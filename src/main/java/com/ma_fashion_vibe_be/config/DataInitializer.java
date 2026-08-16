package com.ma_fashion_vibe_be.config;

import com.ma_fashion_vibe_be.entities.User;
import com.ma_fashion_vibe_be.enums.Provider;
import com.ma_fashion_vibe_be.enums.Role;
import com.ma_fashion_vibe_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Set;

//@Profile({"dev","local"})
@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Bean
    CommandLineRunner initAdmin() {
        return args -> {

            if (userRepository.existsByEmailAndProvider(adminProperties.getEmail(), Provider.LOCAL)) {
                return;
            }

            User admin = User.builder()
                    .email(adminProperties.getEmail())
                    .password(passwordEncoder.encode(adminProperties.getPassword()))
                    .fullName(adminProperties.getName())
                    .provider(Provider.LOCAL)
                    .enabled(true)
                    .roles(Set.of(Role.ADMIN, Role.STAFF, Role.USER))
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);

            System.out.println("Admin initialized");

        };
    }
}
