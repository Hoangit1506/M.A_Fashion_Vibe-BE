package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.cart.CartItemRequest;
import com.ma_fashion_vibe_be.entities.*;
import com.ma_fashion_vibe_be.enums.MediaOwnerType;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    UserRepository userRepository;
    ProductVariantRepository variantRepository;
    MediaRepository mediaRepository;
    InventoryRepository inventoryRepository;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public void addToCart(CartItemRequest request) {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        ProductVariant variant = variantRepository.findById(request.getVariantId()).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Chặn không cho thêm vào giỏ hàng nếu sản phẩm đã ngừng kinh doanh (Xóa mềm) Phải check cả Sản phẩm gốc VÀ Phân loại hàng
        if ((variant.getActive() != null && !variant.getActive()) || !variant.getProduct().isActive()) {
            throw new AppException(ErrorCode.PRODUCT_INACTIVE);
        }

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        // TÍNH TOÁN TỒN KHO THỰC TẾ (Đã rào luôn safetyStock)
        Inventory inventory = inventoryRepository.findByVariantId(variant.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        int availableStock = inventory.getQuantity() - inventory.getReserved() - inventory.getSafetyStock();

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            // KIỂM TRA CHẶN ĐỨNG NẾU VƯỢT TỒN KHO
            if (newQuantity > availableStock) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // KIỂM TRA CHẶN ĐỨNG NẾU VƯỢT TỒN KHO
            if (request.getQuantity() > availableStock) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(request.getQuantity());
            cartItemRepository.save(newItem);
        }
    }

    @Transactional(readOnly = true)
    public Object getMyCart() {
        String userId = getCurrentUserId();
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);

        if (cartOpt.isEmpty() || cartOpt.get().getItems().isEmpty()) {
            return java.util.Map.of("items", java.util.List.of(), "totalAmount", 0);
        }

        Cart cart = cartOpt.get();

        // Biến đổi các Entity CartItem thành DTO để gửi về FE
        var itemResponses = cart.getItems().stream().map(item -> {
            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();

            // Lấy ảnh của biến thể (hoặc ảnh gốc của sản phẩm)
            String imageUrl = null;
            var varMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, variant.getId());
            if (!varMedia.isEmpty()) {
                imageUrl = varMedia.get(0).getUrl();
            } else {
                var prodMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
                if (!prodMedia.isEmpty()) imageUrl = prodMedia.get(0).getUrl();
            }

            // Tính số lượng thực tế trong kho
            int stock = 0;
            var invOpt = inventoryRepository.findByVariantId(variant.getId());
            if (invOpt.isPresent()) {
                Inventory inv = invOpt.get();
                // Trừ cả Safety Stock để UI khóa nút bấm ( + ) nếu khách chọn lố
                stock = inv.getQuantity() - inv.getReserved() - inv.getSafetyStock();
            }

            // Cờ active gửi ra Frontend chỉ TRUE khi cả Cha và Con đều TRUE
            boolean isVariantActive = variant.getActive() != null ? variant.getActive() : true;
            boolean isProductActive = product.isActive();

            // KHẮC PHỤC LỖI MAP.OF BẰNG CÁCH DÙNG HASHMAP
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", item.getId());
            map.put("variantId", variant.getId());
            map.put("productId", product.getId());
            map.put("productName", product.getName());
            map.put("productSlug", product.getSlug());
            map.put("color", variant.getColor());
            map.put("size", variant.getSize());
            map.put("weight", variant.getWeight() != null ? variant.getWeight() : java.math.BigDecimal.ZERO);
            map.put("comparePrice", variant.getComparePrice() != null ? variant.getComparePrice() : variant.getPrice());
            map.put("price", variant.getPrice());
            map.put("quantity", item.getQuantity());
            map.put("maxStock", Math.max(0, stock));
            map.put("imageUrl", imageUrl != null ? imageUrl : "");
            // Trả về trạng thái để Frontend làm mờ hoặc hiện chữ "Ngừng bán"
            map.put("active", isVariantActive && isProductActive);

            return map;
        }).toList();

        // Lọc ra các món đang còn bán (active = true) để tính tiền, không tính tiền món đã xóa mềm
        java.math.BigDecimal total = itemResponses.stream()
                .filter(i -> (Boolean) i.get("active"))
                .map(i -> ((java.math.BigDecimal) i.get("price")).multiply(new java.math.BigDecimal((Integer) i.get("quantity"))))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return java.util.Map.of(
                "cartId", cart.getId(),
                "items", itemResponses,
                "totalAmount", total
        );
    }

    @Transactional
    public void updateCartItem(Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        // Bảo mật: Kiểm tra xem món hàng này có đúng là của User đang đăng nhập không
        if (!item.getCart().getUser().getId().equals(getCurrentUserId())) throw new AppException(ErrorCode.UNAUTHORIZED);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeCartItem(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!item.getCart().getUser().getId().equals(getCurrentUserId())) throw new AppException(ErrorCode.UNAUTHORIZED);
        cartItemRepository.delete(item);
    }

    // =========================================================================
    // CÁC HÀM PHỤC VỤ TÍNH NĂNG GUEST CART (GIỎ HÀNG KHÁCH VÃNG LAI)
    // =========================================================================

    @Transactional(readOnly = true)
    public Object calculatePublicCart(List<CartItemRequest> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) {
            return Map.of("items", List.of(), "totalAmount", 0);
        }

        List<Map<String, Object>> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItemRequest req : guestItems) {
            Optional<ProductVariant> variantOpt = variantRepository.findById(req.getVariantId());
            // NGOẠI LỆ: Nếu Admin đã xóa cứng -> Bỏ qua món này
            if (variantOpt.isEmpty()) continue;

            ProductVariant variant = variantOpt.get();
            Product product = variant.getProduct();

            // Tính tồn kho thực tế để báo về FE
            int stock = 0;
            Optional<Inventory> invOpt = inventoryRepository.findByVariantId(variant.getId());
            if (invOpt.isPresent()) {
                Inventory inv = invOpt.get();
                stock = inv.getQuantity() - inv.getReserved() - inv.getSafetyStock();
            }

            // Lấy ảnh
            String imageUrl = null;
            var varMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, variant.getId());
            if (!varMedia.isEmpty()) {
                imageUrl = varMedia.get(0).getUrl();
            } else {
                var prodMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, product.getId());
                if (!prodMedia.isEmpty()) imageUrl = prodMedia.get(0).getUrl();
            }

            boolean isVariantActive = variant.getActive() != null ? variant.getActive() : true;
            boolean isProductActive = product.isActive();
            boolean active = isVariantActive && isProductActive;

            // MAP DỮ LIỆU CHUẨN (KHÔNG ÉP SỐ LƯỢNG)
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", variant.getId()); // Dùng ID variant làm key tạm cho FE
            map.put("variantId", variant.getId());
            map.put("productId", product.getId());
            map.put("productName", product.getName());
            map.put("productSlug", product.getSlug());
            map.put("color", variant.getColor());
            map.put("size", variant.getSize());
            map.put("weight", variant.getWeight() != null ? variant.getWeight() : java.math.BigDecimal.ZERO);
            map.put("comparePrice", variant.getComparePrice() != null ? variant.getComparePrice() : variant.getPrice());
            map.put("price", variant.getPrice());

            // LƯU Ý 1: Giữ nguyên số lượng của khách để FE bắt lỗi
            map.put("quantity", req.getQuantity());
            map.put("maxStock", Math.max(0, stock));
            map.put("imageUrl", imageUrl != null ? imageUrl : "");
            map.put("active", active);

            itemResponses.add(map);

            // LƯU Ý 2: Tính tổng tiền y như khách yêu cầu (chỉ cộng nếu món hàng còn kinh doanh)
            // Việc khóa nút "Thanh toán" do Frontend phụ trách vì lúc này quantity > maxStock
            if (active) {
                totalAmount = totalAmount.add(variant.getPrice().multiply(new java.math.BigDecimal(req.getQuantity())));
            }
        }

        return java.util.Map.of(
                "cartId", 0, // Mock ID an toàn (Stateless)
                "items", itemResponses,
                "totalAmount", totalAmount
        );
    }

    @Transactional
    public void syncCart(List<CartItemRequest> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) return;

        String userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        for (CartItemRequest req : guestItems) {
            Optional<ProductVariant> variantOpt = variantRepository.findById(req.getVariantId());
            if (variantOpt.isEmpty()) continue; // Bỏ qua xóa cứng

            ProductVariant variant = variantOpt.get();
            Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());

            // LƯU Ý 3: Cứ thoải mái cộng dồn và lưu vào DB.
            // - Lát nữa FE fetch lại giỏ hàng sẽ thấy vượt kho và tự báo đỏ.
            // - OrderService sẽ làm chốt chặn cuối cùng nếu cố tình vượt rào.
            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + req.getQuantity());
                cartItemRepository.save(item);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setVariant(variant);
                newItem.setQuantity(req.getQuantity());
                cartItemRepository.save(newItem);
            }
        }
    }
}