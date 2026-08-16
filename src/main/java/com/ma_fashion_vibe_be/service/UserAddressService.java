package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.user.UserAddressRequest;
import com.ma_fashion_vibe_be.dto.user.UserAddressResponse;
import com.ma_fashion_vibe_be.entities.Address;
import com.ma_fashion_vibe_be.entities.User;
import com.ma_fashion_vibe_be.entities.UserAddress;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.UserAddressRepository;
import com.ma_fashion_vibe_be.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserAddressService {

    UserAddressRepository userAddressRepository;
    UserRepository userRepository;

    public List<UserAddressResponse> getUserAddresses(String userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserAddressResponse createAddress(String userId, UserAddressRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        List<UserAddress> existingAddresses = userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId);
        boolean isDefault = existingAddresses.isEmpty() || (request.getIsDefault() != null && request.getIsDefault());

        if (isDefault && !existingAddresses.isEmpty()) {
            existingAddresses.forEach(addr -> addr.setDefault(false));
            userAddressRepository.saveAll(existingAddresses);
        }

        Address addressEmbed = Address.builder()
                .receiverName(request.getReceiverName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .build();

        UserAddress newUserAddress = UserAddress.builder()
                .user(user)
                .address(addressEmbed)
                .label(request.getLabel())
                .isDefault(isDefault)
                .build();

        UserAddress savedAddress = userAddressRepository.save(newUserAddress);
        return mapToResponse(savedAddress);
    }

    private UserAddressResponse mapToResponse(UserAddress entity) {
        return UserAddressResponse.builder()
                .id(entity.getId())
                .address(entity.getAddress())
                .isDefault(entity.isDefault())
                .label(entity.getLabel())
                .createdAt(entity.getCreatedAt())
                .build();
    }


    @Transactional
    public UserAddressResponse updateAddress(String userId, Long addressId, UserAddressRequest request) {
        UserAddress userAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // Bảo mật: Đảm bảo địa chỉ này thuộc về người đang đăng nhập
        if (!userAddress.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }

        // Cập nhật thông tin
        Address embed = userAddress.getAddress();
        embed.setReceiverName(request.getReceiverName());
        embed.setPhone(request.getPhone());
        embed.setStreet(request.getStreet());

        // Nếu người dùng có chọn lại khu vực mới thì mới cập nhật
        if (request.getProvince() != null && !request.getProvince().isEmpty()) {
            embed.setProvince(request.getProvince());
            embed.setDistrict(request.getDistrict());
            embed.setWard(request.getWard());
        }

        userAddress.setLabel(request.getLabel());

        // Nếu check vào Đặt làm mặc định
        if (request.getIsDefault() != null && request.getIsDefault() && !userAddress.isDefault()) {
            List<UserAddress> allAddresses = userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId);
            allAddresses.forEach(addr -> addr.setDefault(false));
            userAddressRepository.saveAll(allAddresses);
            userAddress.setDefault(true);
        }

        return mapToResponse(userAddressRepository.save(userAddress));
    }

    @Transactional
    public void setDefaultAddress(String userId, Long addressId) {
        List<UserAddress> allAddresses = userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId);
        allAddresses.forEach(addr -> {
            if (addr.getId().equals(addressId)) {
                addr.setDefault(true);
            } else {
                addr.setDefault(false);
            }
        });
        userAddressRepository.saveAll(allAddresses);
    }

    @Transactional
    public void deleteAddress(String userId, Long addressId) {
        UserAddress userAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!userAddress.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }

        if (userAddress.isDefault()) {
            throw new AppException(ErrorCode.ADDRESS_DEFAULT_CANNOT_DELETE);
        }

        userAddressRepository.delete(userAddress);
    }
}