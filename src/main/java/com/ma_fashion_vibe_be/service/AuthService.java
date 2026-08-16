package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.config.JwtProperties;
import com.ma_fashion_vibe_be.dto.auth.*;
import com.ma_fashion_vibe_be.entities.RefreshToken;
import com.ma_fashion_vibe_be.entities.User;
import com.ma_fashion_vibe_be.enums.Provider;
import com.ma_fashion_vibe_be.enums.Role;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.RefreshTokenRepository;
import com.ma_fashion_vibe_be.repository.UserRepository;
import com.ma_fashion_vibe_be.util.ClientInfoUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {
    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    PasswordEncoder passwordEncoder;
    JwtEncoder jwtEncoder;
    JwtProperties jwtProperties;
    EmailService emailService;
    StringRedisTemplate redisTemplate;

    public void sendRegisterOtp(String email) {
        if(userRepository.existsByEmailAndProvider(email, Provider.LOCAL)){
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu vào Redis với tiền tố REG_OTP (Sống 5 phút)
        redisTemplate.opsForValue().set("REG_OTP:" + email, otp, 5, TimeUnit.MINUTES);

        emailService.sendOtpEmail(email, otp);
    }

    @Transactional
    public void register(RegisterRequest request){
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // Vẫn phải check lại tồn tại (phòng hờ trong 5 phút đó có người khác đăng ký mất)
        if(userRepository.existsByEmailAndProvider(request.getEmail(), Provider.LOCAL)){
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Lấy OTP từ Redis ra kiểm tra
        String cachedOtp = redisTemplate.opsForValue().get("REG_OTP:" + request.getEmail());
        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .provider(Provider.LOCAL)
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        userRepository.save(user);

        redisTemplate.delete("REG_OTP:" + request.getEmail());

        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
    }

    public void sendForgotPasswordOtp(String email) {
        // Kiểm tra user có tồn tại và đăng ký bằng LOCAL không
        User user = userRepository.findByEmailAndProvider(email, Provider.LOCAL)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu vào Redis với tiền tố FORGOT_OTP
        redisTemplate.opsForValue().set("FORGOT_OTP:" + email, otp, 5, TimeUnit.MINUTES);

        emailService.sendForgotPasswordEmail(email, otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        User user = userRepository.findByEmailAndProvider(request.getEmail(), Provider.LOCAL)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy mã OTP khôi phục từ Redis
        String cachedOtp = redisTemplate.opsForValue().get("FORGOT_OTP:" + request.getEmail());
        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa mã OTP
        redisTemplate.delete("FORGOT_OTP:" + request.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        User user = userRepository
                .findByEmailAndProvider(request.getEmail(), Provider.LOCAL)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if(!user.isEnabled()){
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = createAccessToken(user);

        String refreshToken = createRefreshToken(user, httpServletRequest);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(jwtProperties.getAccessTokenExpMinutes()*60)
                .build();
    }

    @Transactional
    public AuthResponse googleLogin(String idToken, HttpServletRequest httpServletRequest) {
        // 1. Gọi lên Google để kiểm tra tính hợp lệ của Token
        RestTemplate restTemplate = new RestTemplate();
        String googleUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            Map<String, Object> googleProfile = restTemplate.getForObject(googleUrl, Map.class);
            if (googleProfile == null || !googleProfile.containsKey("email")) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String email = (String) googleProfile.get("email");
            String name = (String) googleProfile.get("name");

            // 2. Kiểm tra xem User này đã có trong DB chưa
            User user = userRepository.findByEmailAndProvider(email, Provider.GOOGLE)
                    .orElseGet(() -> {
                        // Nếu chưa có, tự động tạo tài khoản mới cho khách
                        User newUser = User.builder()
                                .email(email)
                                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ảo bảo mật
                                .fullName(name)
                                .provider(Provider.GOOGLE)
                                .enabled(true) // Google xác thực rồi nên mở khóa luôn
                                .roles(Set.of(Role.USER))
                                .build();
                        return userRepository.save(newUser);
                    });

            // Nếu tài khoản Google này đã bị Admin khóa -> Báo lỗi không cho vào
            if (!user.isEnabled()) {
                throw new AppException(ErrorCode.USER_DISABLED);
            }

            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            // 3. Cấp Token của hệ thống M.A Fashion
            String accessToken = createAccessToken(user);
            String refreshToken = createRefreshToken(user, httpServletRequest);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiry(jwtProperties.getAccessTokenExpMinutes() * 60)
                    .build();

        } catch (AppException e) {
            throw e; // Nếu là lỗi của hệ thống (như USER_DISABLED), ném thẳng ra ngoài luôn!
        } catch (Exception e) {
            // Các lỗi khác (như token google hết hạn, đứt mạng...) thì mới báo UNAUTHENTICATED
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Transactional
    public void logout(String refreshToken){

        String hash = hashToken(refreshToken);

        RefreshToken token = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        refreshTokenRepository.delete(token);
    }

    private String createAccessToken(User user){
        Instant now = Instant.now();

        Instant expiry = now.plus(jwtProperties.getAccessTokenExpMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ma-fashion-vibe")
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .claim("provider", user.getProvider().name())
                .issuedAt(now)
                .expiresAt(expiry)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
//        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();  sai do trong SecurityConfig, bạn đang tạo một Key sử dụng thuật toán HS256 (HMAC SHA-256). Tuy nhiên, ở AuthService, khi bạn gọi JwtEncoderParameters.from(claims), Spring Security sẽ lấy RS256 (thuật toán RSA mặc định) để gán vào Header của JWT. Khi NimbusJwtEncoder tìm kiếm trong jwkSource một key hỗ trợ RS256 nhưng chỉ thấy key HS256, nó sẽ báo lỗi Failed to select a JWK signing key.
    }

    private String createRefreshToken(User user, HttpServletRequest httpServletRequest){
        refreshTokenRepository.deleteByUser(user);
// THÊM DÒNG NÀY: Ép Hibernate thực thi lệnh DELETE xuống Database ngay lập tức
        refreshTokenRepository.flush();
        String rawToken = UUID.randomUUID().toString();

        String hash = hashToken(rawToken);

        Instant expiry = Instant.now()
                .plus(jwtProperties.getRefreshTokenExpDays(), ChronoUnit.DAYS);

        String ipAddress = ClientInfoUtils.getClientIp(httpServletRequest);
        String device = ClientInfoUtils.getClientDevice(httpServletRequest);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(expiry)
                .revoked(false)
                .ipAddress(ipAddress)
                .device(device)
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        String hash = hashToken(request.getRefreshToken());

        RefreshToken token = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        if(token.getExpiresAt().isBefore(Instant.now())){
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = token.getUser();

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        String newRefreshToken = createRefreshToken(user, httpServletRequest);

        String newAccessToken = createAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiry(jwtProperties.getAccessTokenExpMinutes() * 60)
                .build();
    }

    private String hashToken(String token){

        try{

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes());

            return Base64.getEncoder().encodeToString(hash);

        }catch(Exception e){

            throw new AppException(ErrorCode.TOKEN_HASH_ERROR);
        }
    }
}
