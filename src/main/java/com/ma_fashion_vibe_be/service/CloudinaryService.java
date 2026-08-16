package com.ma_fashion_vibe_be.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryService {

    Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String folderName) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        long sizeInBytes = file.getSize();

        if (contentType != null) {
            if (folderName.contains("variants") && !contentType.startsWith("image/")) {
                throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
            }
            // Nếu là ảnh, tối đa 10MB (10 * 1024 * 1024 bytes)
            if (contentType.startsWith("image/") && sizeInBytes > 10 * 1024 * 1024) {
                throw new AppException(ErrorCode.IMAGE_SIZE_EXCEEDED);
            }
            // Nếu là video, tối đa 100MB (100 * 1024 * 1024 bytes)
            else if (contentType.startsWith("video/") && sizeInBytes > 100 * 1024 * 1024) {
                throw new AppException(ErrorCode.VIDEO_SIZE_EXCEEDED);
            }
        }

        try {
            String publicId = UUID.randomUUID().toString();
            String cloudinaryFolder = "ma_fashion_vibe/" + folderName;

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", cloudinaryFolder,
                    "resource_type", "auto"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_IMAGE_FAILED);
        }
    }

    public void deleteImage(String imageUrl) {
//        try {
//            String publicId = imageUrl.substring(imageUrl.indexOf("ma_fashion_vibe/"), imageUrl.lastIndexOf("."));
//            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
//        } catch (Exception e) {
//            throw new AppException(ErrorCode.DELETE_IMAGE_FAILED);
//        }

        try {
            // Cắt lấy Public ID từ URL của Cloudinary
            String publicId = imageUrl.substring(imageUrl.indexOf("ma_fashion_vibe/"), imageUrl.lastIndexOf("."));

            // Tự động nhận diện resource_type để Cloudinary xóa đúng định dạng (Image hay Video)
            String resourceType = "image";
            String lowerUrl = imageUrl.toLowerCase();
            if (lowerUrl.matches(".*\\.(mp4|webm|mov|ogg|avi|wmv|mkv)(\\?.*)?") || lowerUrl.contains("/video/upload/")) {
                resourceType = "video";
            }

            // Truyền thêm option resourceType vào lệnh xóa
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception e) {
            throw new AppException(ErrorCode.DELETE_IMAGE_FAILED);
        }
    }
}