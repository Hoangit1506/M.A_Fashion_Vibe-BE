package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.user.*;
import com.ma_fashion_vibe_be.entities.User;
import com.ma_fashion_vibe_be.enums.Provider;
import com.ma_fashion_vibe_be.enums.Role;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.mapper.UserMapper;
import com.ma_fashion_vibe_be.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse getMyInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void updateMyProfile(String userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Cập nhật thông tin
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setDob(request.getDob());

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        // 1. Kiểm tra 2 mật khẩu mới có khớp nhau không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // 2. Tìm User trong Database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));


        // 3. Kiểm tra Mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. Mã hóa mật khẩu mới và lưu vào DB
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }


    // Lấy danh sách cho Admin (Có Search, Filter, Pagination)
    public Page<UserAdminResponse> getUsersForAdmin(int page, int size, String keyword, Role role, Boolean enabled, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort); // DB đếm từ 0, Frontend đếm từ 1

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        String roleStr = (role != null) ? role.name() : null;
        Page<User> usersPage = userRepository.findUsersForAdmin(searchKeyword, roleStr, Role.ADMIN, Role.STAFF, enabled, pageable);

        // Map từ Entity sang Response
        return usersPage.map(user -> UserAdminResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .dob(user.getDob())
                .provider(user.getProvider().name())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build());
    }

    // 2. Admin tạo tài khoản Staff
    @Transactional
    public void createStaffAccount(CreateStaffRequest request) {
        if (userRepository.existsByEmailAndProvider(request.getEmail(), Provider.LOCAL)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        User staff = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .provider(Provider.LOCAL)
                .enabled(true)
                .roles(Set.of(Role.STAFF, Role.USER))
                .build();

        userRepository.save(staff);
    }

    // 3. Admin khóa hoặc mở khóa tài khoản
    @Transactional
    public void toggleUserStatus(String userId, String currentAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Logic bảo vệ: Không cho phép Admin tự khóa chính mình
        if (user.getId().equals(currentAdminId) || user.getEmail().equals(currentAdminId)) {
            throw new AppException(ErrorCode.CANNOT_LOCK_YOURSELF);
        }

        // Logic bảo vệ: Không cho phép khóa tài khoản của 1 Admin khác (nếu có nhiều Admin)
        if (user.getRoles().contains(Role.ADMIN)) {
            throw new AppException(ErrorCode.CANNOT_LOCK_ADMIN);
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }
}
