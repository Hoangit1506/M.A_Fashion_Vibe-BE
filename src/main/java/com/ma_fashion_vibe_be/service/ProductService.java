package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.product.*;
import com.ma_fashion_vibe_be.entities.*;
import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import com.ma_fashion_vibe_be.enums.MediaType;
import com.ma_fashion_vibe_be.enums.StockChangeType;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.*;
import com.ma_fashion_vibe_be.util.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {

    ProductRepository productRepository;
    ProductVariantRepository variantRepository;
    InventoryRepository inventoryRepository;
    InventoryLogRepository inventoryLogRepository;
    MediaRepository mediaRepository;
    CategoryRepository categoryRepository;
    OrderItemRepository orderItemRepository;
    CartItemRepository cartItemRepository;
    CloudinaryService cloudinaryService;

    // --- HÀM BỔ TRỢ: TẠO SLUG CHUẨN SEO ---
    private String generateUniqueSlug(String name, Category category, String brand) {
        StringBuilder slugBuilder = new StringBuilder();

        if (category != null && category.getName() != null) {
            slugBuilder.append(category.getName()).append(" ");
        }
        if (brand != null && !brand.trim().isEmpty()) {
            slugBuilder.append(brand).append(" ");
        }
        slugBuilder.append(name);

        String baseSlug = StringUtils.toSlug(slugBuilder.toString());
        String finalSlug = baseSlug;

        // Chống trùng lặp
        if (productRepository.existsBySlug(finalSlug)) {
            finalSlug = baseSlug + "-" + java.util.UUID.randomUUID().toString().substring(0, 6);
        }
        return finalSlug;
    }

    // --- HÀM BỔ TRỢ: KIỂM TRA ĐỊNH DẠNG FILE LÀ ẢNH HAY VIDEO ---
    private MediaType determineMediaType(String url) {
        if (url == null || url.trim().isEmpty()) return MediaType.IMAGE; // Mặc định
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.matches(".*\\.(mp4|webm|mov|ogg)(\\?.*)?") || lowerUrl.contains("/video/upload/")) {
            return MediaType.VIDEO;
        }
        return MediaType.IMAGE;
    }

    // HÀM LẤY DANH SÁCH SẢN PHẨM CÓ PHÂN TRANG VÀ LỌC
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getAllProducts(int page, int size, String keyword, Long categoryId, String sortBy, String direction, Boolean isActive) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        List<Long> categoryIds = null;
        if (categoryId != null) {
            categoryIds = new ArrayList<>();
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

        // Truyền thêm isActive vào Repository
        Page<Product> productPage = productRepository.searchProducts(keyword, categoryIds, isActive, pageable);

        return productPage.map(product -> {
            List<Media> mediaList = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
            String thumbnail = mediaList.isEmpty() ? null : mediaList.get(0).getUrl();

            List<ProductVariant> activeVariants = product.getVariants().stream()
                    .filter(v -> v.getActive() != null && v.getActive())
                    .toList();

            BigDecimal minPrice = activeVariants.stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = activeVariants.stream().map(ProductVariant::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal comparePrice = activeVariants.stream().map(ProductVariant::getComparePrice).filter(p -> p != null).max(BigDecimal::compareTo).orElse(null);

            Integer totalStock = inventoryRepository.getTotalStockByProductId(product.getId());

            // ĐÃ THÊM: Tính tổng số lượng đang giữ chỗ của tất cả Variant trong Product này
            int totalReserved = product.getVariants().stream()
                    .mapToInt(v -> {
                        var invOpt = inventoryRepository.findByVariantId(v.getId());
                        return invOpt.isPresent() ? invOpt.get().getReserved() : 0;
                    })
                    .sum();

            return ProductListResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .slug(product.getSlug())
                    .brand(product.getBrand())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Không có")
                    .thumbnail(thumbnail)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .comparePrice(comparePrice)
                    .soldCount(product.getSoldCount())
                    .ratingAvg(product.getRatingAvg())
                    .totalStock(totalStock)
                    .totalReserved(totalReserved)
                    .active(product.isActive())
                    .createdAt(product.getCreatedAt().toString())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailPublic(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isActive()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // Lấy toàn bộ ảnh chung
        List<Media> mediaList = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
        List<String> imageUrls = mediaList.stream().map(Media::getUrl).toList();

        // BIẾN ĐỔI ENTITY THÀNH DTO VÀ TÍNH TỒN KHO THỰC TẾ
        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .filter(variant -> variant.getActive() != null && variant.getActive()) // Chỉ lấy cái đang bán
                .map(variant -> {

            // Tìm số lượng trong kho của Variant này
            int availableStock = 0;
            var inventoryOpt = inventoryRepository.findByVariantId(variant.getId());
            if (inventoryOpt.isPresent()) {
                Inventory inv = inventoryOpt.get();
                // ĐÃ CẬP NHẬT: Trừ thêm Safety Stock ở đây để truyền xuống Frontend
                availableStock = inv.getQuantity() - inv.getReserved() - inv.getSafetyStock();
            }

            // Lấy ảnh riêng của Variant (nếu có)
            String variantImage = null;
            List<Media> varMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, variant.getId());
            if (!varMedia.isEmpty()) {
                variantImage = varMedia.get(0).getUrl();
            }

            return ProductVariantResponse.builder()
                    .id(variant.getId())
                    .sku(variant.getSku())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .price(variant.getPrice())
                    .comparePrice(variant.getComparePrice())
                    .stockQuantity(Math.max(0, availableStock)) // Đảm bảo kho không bị âm
                    .imageUrl(variantImage)
                    .build();
        }).toList();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                .brand(product.getBrand())
                .soldCount(product.getSoldCount())
                .ratingAvg(product.getRatingAvg())
                .imageUrls(imageUrls)
                .variants(variantResponses) // Trả về danh sách DTO mới
                .build();
    }

    @Transactional
    public void createProduct(ProductCreateRequest request, String adminUserId) {

        // BƯỚC 1: TẠO "VỎ BỌC" SẢN PHẨM (PRODUCT)
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Tạo Slug tự động từ tên Sản phẩm
        String slug = generateUniqueSlug(request.getName(), category, request.getBrand());

        BigDecimal minPrice = request.getVariants().stream()
                .map(VariantRequest::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .category(category)
                .brand(request.getBrand())
                .active(request.getActive() != null ? request.getActive() : true)
                .soldCount(0L)
                .reviewCount(0L)
                .ratingAvg(0.0)
                .minPrice(minPrice)
                .build();

        // Lưu Vỏ bọc vào Database để lấy Product ID
        product = productRepository.save(product);

        // BƯỚC 2: LƯU HÌNH ẢNH VÀ VIDEO CHUNG CỦA SẢN PHẨM (MEDIA)
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<Media> mediaList = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String mediaUrl = request.getImageUrls().get(i);
                // ĐÃ SỬA: Tự động nhận diện Image hay Video
                MediaType mediaType = determineMediaType(mediaUrl);

                // Rào chắn bảo vệ Backend (Không cho Video làm ảnh bìa)
                if (i == 0 && mediaType == MediaType.VIDEO) {
                    throw new AppException(ErrorCode.COVER_IMAGE_REQUIRED);
                }

                Media media = Media.builder()
                        .url(mediaUrl)
                        .type(mediaType)
                        .ownerType(MediaOwnerType.PRODUCT)
                        .ownerId(product.getId())
                        .isPrimary(i == 0)
                        .position(i + 1)
                        .build();
                mediaList.add(media);
            }
            mediaRepository.saveAll(mediaList);
        }

        // BƯỚC 3: TẠO BIẾN THỂ & NHẬP KHO (VARIANTS & INVENTORY)
        for (VariantRequest vReq : request.getVariants()) {

            // 3.1. Lưu Biến thể
            String sku = vReq.getSku();
            if (variantRepository.existsBySku(sku)) {
                throw new AppException(ErrorCode.VARIANT_DUPLICATE_SKU);
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(product) // Thuộc về sản phẩm ở Bước 1
                    .sku(sku)
                    .size(vReq.getSize())
                    .color(vReq.getColor())
                    .price(vReq.getPrice())
                    .comparePrice(vReq.getComparePrice())
                    .weight(vReq.getWeight())
                    .build();

            variant = variantRepository.save(variant);

            // 3.1 Lưu ảnh riêng của Variant (NẾU CÓ)
            if (vReq.getImageUrl() != null && !vReq.getImageUrl().isEmpty()) {
                // RÀO CHẮN: Cấm up Video làm ảnh đại diện phân loại
                if (determineMediaType(vReq.getImageUrl()) == MediaType.VIDEO) {
                    throw new AppException(ErrorCode.INVALID_MEDIA_TYPE); // Dùng lại mã lỗi cũ của bạn
                }

                Media variantMedia = Media.builder()
                        .url(vReq.getImageUrl())
                        .type(MediaType.IMAGE) // Chắc chắn là ảnh rồi
                        .ownerType(MediaOwnerType.PRODUCT_VARIANT)
                        .ownerId(variant.getId())
                        .isPrimary(true)
                        .position(1)
                        .build();
                mediaRepository.save(variantMedia);
            }

            // 3.2. Khởi tạo Kho hàng (Inventory) cho biến thể này
            Inventory inventory = Inventory.builder()
                    .variant(variant)
                    .quantity(vReq.getStockQuantity()) // Lấy số lượng từ Form của Admin
                    .reserved(0)                       // Mới tạo nên chưa có ai đặt giữ hàng
                    .safetyStock(5)                    // Mức cảnh báo sắp hết hàng (hardcode tạm)
                    .build();
            inventoryRepository.save(inventory);

            // 3.3. Ghi chép vào Nhật ký Kho (Camera an ninh)
            InventoryLog inventoryLog = InventoryLog.builder()
                    .variantId(variant.getId())
                    .changeQuantity(vReq.getStockQuantity())
                    .changeType(StockChangeType.IMPORT) // Loại: Nhập hàng
                    .note("Khởi tạo sản phẩm mới")
                    .performedByUserId(adminUserId) // Lưu lại Admin nào đã tạo sản phẩm này
                    .build();
            inventoryLogRepository.save(inventoryLog);
        }
    }

    // HÀM 1: LẤY CHI TIẾT DÀNH RIÊNG CHO ADMIN (Không lọc active)
    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<Media> mediaList = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
        List<String> imageUrls = mediaList.stream().map(Media::getUrl).toList();

        List<ProductVariantResponse> variantResponses = product.getVariants().stream().map(variant -> {
            int availableStock = 0;
            int reservedStock = 0;
            var inventoryOpt = inventoryRepository.findByVariantId(variant.getId());
            if (inventoryOpt.isPresent()) {
                availableStock = inventoryOpt.get().getQuantity();
                reservedStock = inventoryOpt.get().getReserved();
            }

            String variantImage = null;
            List<Media> varMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, variant.getId());
            if (!varMedia.isEmpty()) variantImage = varMedia.get(0).getUrl();

            return ProductVariantResponse.builder()
                    .id(variant.getId())
                    .sku(variant.getSku())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .price(variant.getPrice())
                    .comparePrice(variant.getComparePrice())
                    .stockQuantity(Math.max(0, availableStock))
                    .reserved(reservedStock)
                    .imageUrl(variantImage)
                    .active(variant.getActive() != null ? variant.getActive() : true)
                    .build();
        }).toList();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .brand(product.getBrand())
                .active(product.isActive())
                .imageUrls(imageUrls)
                .variants(variantResponses)
                .build();
    }


    // HÀM XỬ LÝ CẬP NHẬT
    @Transactional
    public void updateProduct(Long productId, ProductUpdateRequest request, String adminUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 1. CẬP NHẬT VỎ SẢN PHẨM & TẠO SLUG MỚI NẾU CẦN
        // Dùng Objects.equals để so sánh Brand an toàn kể cả khi Brand là null
        boolean isSlugChanged = !product.getName().equals(request.getName()) ||
                !product.getCategory().getId().equals(category.getId()) ||
                !Objects.equals(product.getBrand(), request.getBrand());

        product.setName(request.getName());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setDescription(request.getDescription());
        if (request.getActive() != null) product.setActive(request.getActive());

        if (isSlugChanged) {
            product.setSlug(generateUniqueSlug(request.getName(), category, request.getBrand()));
        }
        productRepository.save(product);

        // 2. XỬ LÝ ẢNH GỐC SẢN PHẨM (Dọn rác Cloudinary)
        List<Media> oldProductMedias = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
        List<String> newUrls = request.getImageUrls() != null ? request.getImageUrls() : new java.util.ArrayList<>();

        // Tìm những ảnh cũ CÓ TRONG DB nhưng KHÔNG GỬI LÊN nữa -> Đây là rác, gọi Cloudinary xóa vật lý
        for (Media oldMedia : oldProductMedias) {
            if (!newUrls.contains(oldMedia.getUrl())) {
                try { cloudinaryService.deleteImage(oldMedia.getUrl()); } catch (Exception e) {}
            }
        }

        // Xóa sạch data ảnh gốc trong DB và Insert lại mảng mới theo đúng thứ tự
        mediaRepository.deleteByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCT, product.getId());
        if (!newUrls.isEmpty()) {
            List<Media> newMediasToSave = new java.util.ArrayList<>();
            for (int i = 0; i < newUrls.size(); i++) {
                String mediaUrl = newUrls.get(i);
                // Tự động phân loại ẢNH hay VIDEO
                MediaType mediaType = determineMediaType(mediaUrl);

                if (i == 0 && mediaType == MediaType.VIDEO) {
                    throw new AppException(ErrorCode.COVER_IMAGE_REQUIRED);
                }

                newMediasToSave.add(Media.builder()
                        .url(mediaUrl)
                        .type(mediaType)
                        .ownerType(MediaOwnerType.PRODUCT)
                        .ownerId(product.getId())
                        .isPrimary(i == 0)
                        .position(i + 1)
                        .build());
            }
            mediaRepository.saveAll(newMediasToSave);
        }

        // 3. XỬ LÝ PHÂN LOẠI HÀNG (Variants)
        List<ProductVariant> oldVariants = new java.util.ArrayList<>(product.getVariants());

        for (VariantUpdateRequest vReq : request.getVariants()) {
            if (vReq.getId() == null) {
                // TRƯỜNG HỢP 3.1: THÊM MỚI VARIANT
                if (variantRepository.existsBySku(vReq.getSku())) throw new AppException(ErrorCode.VARIANT_DUPLICATE_SKU);

                ProductVariant newVariant = ProductVariant.builder()
                        .product(product).sku(vReq.getSku()).size(vReq.getSize()).color(vReq.getColor())
                        .price(vReq.getPrice()).comparePrice(vReq.getComparePrice()).weight(vReq.getWeight())
                        .active(vReq.getActive() != null ? vReq.getActive() : true).build();
                newVariant = variantRepository.save(newVariant);

                if (vReq.getImageUrl() != null && !vReq.getImageUrl().isEmpty()) {
                    // ĐÃ SỬA: Chặn nếu Admin cố tình up Video làm ảnh phân loại
                    if (determineMediaType(vReq.getImageUrl()) == MediaType.VIDEO) {
                        throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
                    }

                    mediaRepository.save(Media.builder().url(vReq.getImageUrl()).type(MediaType.IMAGE)
                            .ownerType(MediaOwnerType.PRODUCT_VARIANT).ownerId(newVariant.getId()).isPrimary(true).position(1).build());
                }

                int initStock = vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0;
                inventoryRepository.save(Inventory.builder().variant(newVariant).quantity(initStock).reserved(0).safetyStock(5).build());
                inventoryLogRepository.save(InventoryLog.builder().variantId(newVariant.getId()).changeQuantity(initStock)
                        .changeType(com.ma_fashion_vibe_be.enums.StockChangeType.IMPORT).note("Thêm phân loại lúc sửa SP").performedByUserId(adminUserId).build());

            } else {
                // TRƯỜNG HỢP 3.2: SỬA VARIANT CŨ
                ProductVariant existingVariant = oldVariants.stream().filter(v -> v.getId().equals(vReq.getId())).findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                if (variantRepository.existsBySkuAndIdNot(vReq.getSku(), vReq.getId())) throw new AppException(ErrorCode.VARIANT_DUPLICATE_SKU);

                existingVariant.setSku(vReq.getSku());
                existingVariant.setSize(vReq.getSize());
                existingVariant.setColor(vReq.getColor());
                existingVariant.setPrice(vReq.getPrice());
                existingVariant.setComparePrice(vReq.getComparePrice());
                existingVariant.setWeight(vReq.getWeight());
                if (vReq.getActive() != null) existingVariant.setActive(vReq.getActive());

                variantRepository.save(existingVariant);

                // Xử lý đổi ảnh Variant
                List<Media> oldVarMedias = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, existingVariant.getId());
                String oldVarUrl = oldVarMedias.isEmpty() ? null : oldVarMedias.get(0).getUrl();
                String newVarUrl = vReq.getImageUrl();

                if (oldVarUrl != null && !oldVarUrl.equals(newVarUrl)) {
                    // CÓ ẢNH CŨ MÀ GỬI LÊN ẢNH MỚI KHÁC HOẶC NULL -> RÁC
                    try { cloudinaryService.deleteImage(oldVarUrl); } catch (Exception e) {}
                }

                mediaRepository.deleteByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCT_VARIANT, existingVariant.getId());
                if (newVarUrl != null && !newVarUrl.isEmpty()) {
                    // ĐÃ SỬA: Chặn nếu Admin cố tình up Video làm ảnh phân loại
                    if (determineMediaType(newVarUrl) == MediaType.VIDEO) {
                        throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
                    }

                    mediaRepository.save(Media.builder().url(newVarUrl).type(MediaType.IMAGE)
                            .ownerType(MediaOwnerType.PRODUCT_VARIANT).ownerId(existingVariant.getId()).isPrimary(true).position(1).build());
                }

                oldVariants.remove(existingVariant); // Xóa khỏi list chờ dọn dẹp
            }
        }

        // TRƯỜNG HỢP 3.3: DỌN DẸP RÁC (Những Variant Admin bấm XÓA trên Form)
        for (ProductVariant deletedVariant : oldVariants) {
            Long vId = deletedVariant.getId();

            // ĐÃ SỬA UX: Báo lỗi thẳng mặt Admin nếu cố xóa Variant có người mua
            boolean isUsedInOrders = orderItemRepository.existsByVariantId(vId);
            if (isUsedInOrders) {
                throw new AppException(ErrorCode.VARIANT_IN_USE);
            }

            // Nếu không vướng giao dịch -> Xóa tận gốc
            List<Media> trashVarMedias = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, vId);
            if (!trashVarMedias.isEmpty()) {
                try { cloudinaryService.deleteImage(trashVarMedias.get(0).getUrl()); } catch (Exception e) {}
            }

            inventoryLogRepository.deleteByVariantId(vId);
            inventoryRepository.deleteByVariantId(vId);
            cartItemRepository.deleteByVariantId(vId);
            mediaRepository.deleteByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCT_VARIANT, vId);
            variantRepository.delete(deletedVariant);
        }

        // BƯỚC 4 - TÍNH TOÁN LẠI MIN PRICE VÀ LƯU VÀO SẢN PHẨM
        BigDecimal updatedMinPrice = product.getVariants().stream()
                .filter(v -> v.getActive() != null && v.getActive()) // Chỉ lấy các loại đang mở bán
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        product.setMinPrice(updatedMinPrice);
        productRepository.save(product);
    }


    @Transactional
    public void toggleProductActive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Đảo ngược trạng thái (Đang true thì thành false, và ngược lại)
        product.setActive(!product.isActive());
        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // 1. KIỂM TRA DUY NHẤT "TIỀN ÁN" ĐƠN HÀNG (Không quan tâm giỏ hàng)
        boolean hasOrders = orderItemRepository.existsByVariant_Product_Id(id);

        if (hasOrders) {
            throw new AppException(ErrorCode.PRODUCT_IN_USE);
        }

        // 2. DỌN DẸP SẠCH SẼ TRƯỚC KHI XÓA (Kể cả trên Cloudinary)
        List<ProductVariant> variants = product.getVariants();
        for (ProductVariant variant : variants) {
            Long vId = variant.getId();

            // Xóa ảnh Variant trên Cloud
            List<Media> trashVarMedias = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, vId);
            if (!trashVarMedias.isEmpty()) {
                try { cloudinaryService.deleteImage(trashVarMedias.get(0).getUrl()); } catch (Exception e) {}
            }

            inventoryLogRepository.deleteByVariantId(vId);
            inventoryRepository.deleteByVariantId(vId);
            cartItemRepository.deleteByVariantId(vId);
            mediaRepository.deleteByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCT_VARIANT, vId);
        }

        // Xóa ảnh gốc Sản phẩm trên Cloud
        List<Media> oldProductMedias = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, id);
        for (Media oldMedia : oldProductMedias) {
            try { cloudinaryService.deleteImage(oldMedia.getUrl()); } catch (Exception e) {}
        }
        mediaRepository.deleteByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCT, id);

        // 3. RÚT RỄ VÀ TIÊU DIỆT
        variantRepository.deleteAll(variants);
        productRepository.delete(product);
    }
}