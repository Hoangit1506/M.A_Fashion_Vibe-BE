package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.inventory.*;
import com.ma_fashion_vibe_be.entities.*;
import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import com.ma_fashion_vibe_be.enums.StockChangeType;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {

    InventoryRepository inventoryRepository;
    InventoryLogRepository inventoryLogRepository;
    CategoryRepository categoryRepository;
    MediaRepository mediaRepository;
    UserRepository userRepository;

    // 1. HÀM LẤY DANH SÁCH KHO (Có Search, Lọc Danh mục, Sắp xếp)
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(int page, int size, String keyword, Long categoryId, String sortBy, String direction) {
        // Xử lý Sort
        String actualSortBy = sortBy.equals("quantity") ? "quantity" : "updatedAt"; // Mặc định sắp xếp theo Tồn kho hoặc Ngày cập nhật
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(actualSortBy).ascending() : Sort.by(actualSortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Xử lý Lọc Danh mục (Lấy cả con cháu)
        List<Long> categoryIds = new ArrayList<>();
        boolean hasCategory = false;
        if (categoryId != null) {
            hasCategory = true;
            categoryIds.add(categoryId);
            List<Category> children = categoryRepository.findByParentId(categoryId);
            for (Category child : children) {
                categoryIds.add(child.getId());
                List<Category> grandChildren = categoryRepository.findByParentId(child.getId());
                for (Category grandChild : grandChildren) {
                    categoryIds.add(grandChild.getId());
                }
            }
        }

        // Truy vấn DB
        Page<Inventory> inventoryPage = inventoryRepository.searchInventory(keyword, hasCategory, categoryIds, pageable);

        // Chuyển Entity sang DTO
        return inventoryPage.map(inv -> {
            ProductVariant variant = inv.getVariant();
            Product product = variant.getProduct();

            // Lấy ảnh ưu tiên ảnh Variant, không có thì lấy ảnh Product
            String imageUrl = null;
            var varMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, variant.getId());
            if (!varMedia.isEmpty()) {
                imageUrl = varMedia.get(0).getUrl();
            } else {
                var prodMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
                if (!prodMedia.isEmpty()) imageUrl = prodMedia.get(0).getUrl();
            }

            int available = inv.getQuantity() - inv.getReserved() - inv.getSafetyStock();

            return InventoryResponse.builder()
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .productName(product.getName())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : "N/A")
                    .color(variant.getColor())
                    .size(variant.getSize())
                    .imageUrl(imageUrl)
                    .quantity(inv.getQuantity())
                    .reserved(inv.getReserved())
                    .safetyStock(inv.getSafetyStock())
                    .available(Math.max(0, available))
                    .active((variant.getActive() != null ? variant.getActive() : true) && product.isActive())
                    .lastUpdated(inv.getUpdatedAt() != null ? inv.getUpdatedAt().toString() : "")
                    .build();
        });
    }

    // 2. HÀM ĐIỀU CHỈNH KHO (Nhập thêm / Xuất hủy)
    @Transactional
    public void adjustInventory(InventoryAdjustmentRequest request, String adminUserId) {
        Inventory inventory = inventoryRepository.findByVariantId(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Nếu kho cũ chưa có version thì khởi tạo = 0L
        if (inventory.getVersion() == null) {
            inventory.setVersion(0L);
        }

        int qty = request.getChangeQuantity();
        StockChangeType type = request.getChangeType();

        // BẢO MẬT NGHIỆP VỤ: Nhập mới và Khách trả hàng BẮT BUỘC phải là số dương (>0)
        if ((type == StockChangeType.IMPORT || type == StockChangeType.RETURN) && qty <= 0) {
            throw new IllegalArgumentException("Số lượng nhập kho hoặc khách trả hàng phải là số dương!");
        }

        int newQuantity = inventory.getQuantity() + qty;

        // Cú chốt chặn cuối cùng: Không được phép xuất âm kho!
        if (newQuantity < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        // --- TRA CỨU TÊN NGƯỜI DÙNG TỪ ID ---
        String adminName = adminUserId; // Mặc định nếu lỗi thì xài ID
        var adminOpt = userRepository.findById(adminUserId);
        if (adminOpt.isPresent() && adminOpt.get().getFullName() != null) {
            adminName = adminOpt.get().getFullName(); // Lấy tên thật của Admin
        }

        // --- TỰ ĐỘNG TẠO GHI CHÚ CHUẨN XÁC ---
        ProductVariant variant = inventory.getVariant();
        Product product = variant.getProduct();
        String productName = product.getName();
        String sku = variant.getSku();
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Không xác định";

        // Lấy thời gian thực tế của máy chủ (Giờ Việt Nam)
        String timeStr = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(java.time.Instant.now());

        String autoNote;
        if (type == StockChangeType.IMPORT) {
            // ĐÃ SỬA: Dùng adminName thay vì adminUserId
            autoNote = String.format("Nhập thêm +%d sản phẩm %s, mã %s, thuộc %s vào %s do %s",
                    qty, productName, sku, categoryName, timeStr, adminName);
        } else if (type == StockChangeType.RETURN) {
            // ĐÃ SỬA: Dùng adminName thay vì adminUserId
            autoNote = String.format("Khách hàng hoàn trả lại +%d sản phẩm %s, mã %s, thuộc %s vào %s do %s",
                    qty, productName, sku, categoryName, timeStr, adminName);
        } else {
            // Nghiệp vụ ADJUSTMENT (Kiểm kho/Điều chỉnh)
            String baseNote = (request.getNote() != null && !request.getNote().trim().isEmpty()) ? request.getNote() : "Kiểm kho / Điều chỉnh";
            autoNote = String.format("%s vào %s do %s", baseNote, timeStr, adminName);
        }

        // Ghi nhận vào Camera an ninh (Log)
        InventoryLog log = InventoryLog.builder()
                .variantId(request.getVariantId())
                .changeQuantity(qty)
                .changeType(type)
                .note(autoNote) // Lưu câu Note siêu đẹp đã được gen tự động
                .performedByUserId(adminUserId) // Cột Database vẫn lưu ID cứng để chuẩn kỹ thuật
                .build();
        inventoryLogRepository.save(log);
    }
}