package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.order.OrderAdminResponse;
import com.ma_fashion_vibe_be.dto.order.OrderRequest;
import com.ma_fashion_vibe_be.dto.order.OrderResponse;
import com.ma_fashion_vibe_be.entities.*;
import com.ma_fashion_vibe_be.enums.*;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    UserRepository userRepository;
    CartRepository cartRepository;
    UserAddressRepository addressRepository;
    OrderRepository orderRepository;
    OrderItemRepository orderItemRepository;
    InventoryRepository inventoryRepository;
    InventoryLogRepository inventoryLogRepository;
    MediaRepository mediaRepository;
    ReviewRepository reviewRepository;
    CartItemRepository cartItemRepository;
    ProductRepository productRepository;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Hàm tạo mã đơn hàng ngẫu nhiên (Ví dụ: ORD-12345678)
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 1. Lấy Giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        List<CartItem> cartItems = cart.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY); // Không có hàng để đặt
        }

        // 2. Lấy Địa chỉ giao hàng
        UserAddress userAddress = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_INVALID));

        // 3. Tính toán Tồn kho và Tính Tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalWeightGrams = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();

            // Chặn thanh toán nếu có món bị xóa mềm Phải check cả Sản phẩm gốc VÀ Phân loại hàng
            if ((variant.getActive() != null && !variant.getActive()) || !variant.getProduct().isActive()) {
                throw new AppException(ErrorCode.PRODUCT_INACTIVE);
            }

            // Xử lý giữ chỗ kho (Reserve)
            Inventory inventory = inventoryRepository.findByVariantId(variant.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

            // ĐÃ THÊM TRỪ ĐI SAFETY STOCK Ở ĐÂY
            int availableStock = inventory.getQuantity() - inventory.getReserved() - inventory.getSafetyStock();

            if (item.getQuantity() > availableStock) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK); // Lỗi: Hết hàng cho phép bán
            }

            // Cập nhật Inventory: Cộng vào Reserved  Ghi nhận trừ kho (Nếu ai đó mua cùng lúc, nó sẽ quăng OptimisticLockingFailureException ở đây)
            inventory.setReserved(inventory.getReserved() + item.getQuantity());
            inventoryRepository.save(inventory);

            // GHI LOG CHUẨN XÁC
            InventoryLog log = InventoryLog.builder()
                    .variantId(variant.getId())
                    .changeQuantity(-item.getQuantity())
                    .changeType(StockChangeType.RESERVE)
                    .note("Giữ chỗ cho đơn hàng đang đặt")
                    .performedByUserId(userId)
                    .build();
            inventoryLogRepository.save(log);

            // Tính tiền và cân nặng
            BigDecimal lineTotal = variant.getPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            if (variant.getWeight() != null) {
                BigDecimal weight = variant.getWeight().multiply(new BigDecimal(item.getQuantity()));
                totalWeightGrams = totalWeightGrams.add(weight);
            }

            // Truy xuất Hình ảnh để làm Snapshot
            // Ưu tiên lấy ảnh phân loại, nếu không có thì lấy ảnh bìa của sản phẩm gốc
            String snapshotImage = null;
            var varMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT_VARIANT, variant.getId());
            if (!varMedia.isEmpty()) {
                snapshotImage = varMedia.get(0).getUrl();
            } else {
                var prodMedia = mediaRepository.findByOwnerTypeAndOwnerIdOrderByPositionAsc(MediaOwnerType.PRODUCT, variant.getProduct().getId());
                if (!prodMedia.isEmpty()) snapshotImage = prodMedia.get(0).getUrl();
            }

            // Tạo OrderItem snapshot (Lưu cứng thông tin lúc bán)
            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .productNameSnapshot(variant.getProduct().getName())
                    .skuSnapshot(variant.getSku())
                    .colorSnapshot(variant.getColor())
                    .sizeSnapshot(variant.getSize())
                    .imageUrlSnapshot(snapshotImage)
                    .unitPriceSnapshot(variant.getPrice())
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .build();

            orderItems.add(orderItem);
        }

        // 4. Tính Phí Giao Hàng
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (totalAmount.compareTo(new BigDecimal("500000")) < 0) {
            // Đơn < 500k -> Tính ship: 30k + 5k/kg
            BigDecimal totalWeightKg = totalWeightGrams.divide(new BigDecimal("1000"));
            BigDecimal weightFee = totalWeightKg.multiply(new BigDecimal("5000"));
            shippingFee = new BigDecimal("30000").add(weightFee);
        }

        // 5. Lưu Order
        // TRÍCH XUẤT ĐỊA CHỈ VÀ GẮN GHI CHÚ
        Address orderAddress = Address.builder()
                .receiverName(userAddress.getAddress().getReceiverName())
                .phone(userAddress.getAddress().getPhone())
                .province(userAddress.getAddress().getProvince())
                .district(userAddress.getAddress().getDistrict())
                .ward(userAddress.getAddress().getWard())
                .street(userAddress.getAddress().getStreet())
                .note(request.getNote()) // Đưa ghi chú từ request vào đây
                .build();

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .shippingAddress(orderAddress) // Dùng address mới đã có ghi chú
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Lưu các Items và móc nó vào Order
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
            orderItemRepository.save(orderItem);
        }

        // 6. Xóa sạch Giỏ hàng
        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.builder()
                .id(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .totalAmount(savedOrder.getTotalAmount())
                .shippingFee(savedOrder.getShippingFee())
                .discount(savedOrder.getDiscount())
                .status(savedOrder.getStatus())
                .paymentMethod(savedOrder.getPaymentMethod())
                .paymentStatus(savedOrder.getPaymentStatus())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    @Transactional
    public void processVnpayCallback(String orderNumber, String responseCode, String vnpAmount) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElse(null);

        if (order == null) return; // Không tìm thấy đơn thì bỏ qua

        // Rào chắn 1: Nếu đơn đã PAID rồi thì bỏ qua luôn (tránh khách F5 load lại trang)
        if (order.getPaymentStatus() == PaymentStatus.PAID) return;

        // RÀO CHẮN 2: BẢO MẬT KIỂM TRA SỐ TIỀN THỰC TRẢ
        // Tính lại tổng tiền của đơn (y hệt lúc lúc Controller tạo link VNPAY: (Tiền hàng + Ship - Giảm giá) * 100)
        long expectedAmount = order.getTotalAmount()
                .add(order.getShippingFee())
                .subtract(order.getDiscount())
                .longValue() * 100;
        // Lấy số tiền thực tế VNPAY báo về (Silent Fallback)
        long actualAmount = 0;
        try {
            if (vnpAmount != null) actualAmount = Long.parseLong(vnpAmount);
        } catch (Exception e) {
            log.error("Lỗi ép kiểu số tiền VNPAY. Cảnh báo: vnpAmount không hợp lệ từ VNPAY: " + vnpAmount);
        }

        // Nếu số tiền VNPAY báo về KHÁC với số tiền đơn hàng -> Dấu hiệu gian lận -> Bẻ lái tín hiệu thành mã lỗi (99) để luồng else xử lý hủy
        if (expectedAmount != actualAmount) {
            responseCode = "99";
        }

        // Nếu mã = 00 tức là khách đã thanh toán tiền thành công
        if ("00".equals(responseCode)) {
            if (order.getStatus() == OrderStatus.PENDING) {
                // Kịch bản đẹp: Đơn vẫn đang chờ, xác nhận thành công
                order.setPaymentStatus(PaymentStatus.PAID);
            } else {
                // Kịch bản lỗi (Race condition): Đơn đã bị Cron Job hủy, nhưng khách vẫn bị trừ tiền.
                // Bắt buộc phải đánh dấu là PAID để kế toán biết có dòng tiền vào, nhưng giữ nguyên status là CANCELED.
                // Cửa hàng nhìn thấy đơn "CANCELED" mà "PAID" sẽ biết đường liên hệ khách để hoàn tiền hoặc khôi phục đơn.
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }
        // Nếu khác 00 tức là khách hủy, giao dịch lỗi...
        else {
            // Chỉ xử lý hủy nếu đơn đang ở trạng thái PENDING
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.CANCELED); // Hủy đơn hàng

                // HOÀN TRẢ LẠI TỒN KHO (Nhả reserved ra)
                for (OrderItem item : order.getItems()) {
                    ProductVariant variant = item.getVariant();
                    Inventory inventory = inventoryRepository.findByVariantId(variant.getId())
                            .orElse(null);

                    if (inventory != null) {
//                        inventory.setReserved(inventory.getReserved() - item.getQuantity());
                        inventory.setReserved(Math.max(0, inventory.getReserved() - item.getQuantity()));

                        inventoryRepository.save(inventory);

                        // Ghi log nhả kho
                        InventoryLog log = InventoryLog.builder()
                                .variantId(variant.getId())
                                .changeQuantity(item.getQuantity())
                                .changeType(StockChangeType.RELEASE)
                                .note(actualAmount != expectedAmount ? "Hệ thống hủy đơn: Sai lệch số tiền thanh toán" : "Khách hủy thanh toán VNPAY, hoàn lại giữ chỗ")
                                .performedByUserId(order.getUser().getId())
                                .build();
                        inventoryLogRepository.save(log);
                    }
                }
            }
        }
        orderRepository.save(order);
    }


    // LẤY DANH SÁCH ĐƠN HÀNG CHO ADMIN
    @Transactional(readOnly = true)
    public Page<OrderAdminResponse> getOrdersForAdmin(int page, int size, String keyword, OrderStatus status, PaymentStatus paymentStatus, String startDateStr, String endDateStr, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Xử lý chuyển đổi chuỗi ngày sang Instant
        Instant startDate = null;
        Instant endDate = null;
        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            }
        } catch (Exception e) {}

        Page<Order> orderPage = orderRepository.searchOrdersForAdmin(keyword, status, paymentStatus, startDate, endDate, pageable);

        return orderPage.map(order -> OrderAdminResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .receiverName(order.getShippingAddress().getReceiverName())
                .phone(order.getShippingAddress().getPhone())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build());
    }

    // CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG VÀ XỬ LÝ KHO
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String adminUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus oldStatus = order.getStatus();
        if (oldStatus == newStatus) return;

        // 1. RÀO CHẮN 1: Đơn đã CANCELED hoặc REFUNDED thì cấm đổi lại
        if (oldStatus == OrderStatus.CANCELED || oldStatus == OrderStatus.REFUNDED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 2. RÀO CHẮN 2: BẮT BUỘC ĐI TỪNG BƯỚC (Cấm nhảy cóc)
        // Quy trình chuẩn: 0 -> 1 -> 2 -> 3 -> 4
        List<OrderStatus> flow = Arrays.asList(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPING, OrderStatus.DELIVERED
        );

        // Nếu không phải là lệnh HỦY hay HOÀN TRẢ, thì bắt buộc phải là bước tiếp theo liền kề
        if (newStatus != OrderStatus.CANCELED && newStatus != OrderStatus.REFUNDED) {
            int oldIdx = flow.indexOf(oldStatus);
            int newIdx = flow.indexOf(newStatus);
            // Nếu vị trí mới không nằm ngay sát vị trí cũ -> Lỗi nhảy cóc
            if (newIdx != oldIdx + 1) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }

        // 3. NGHIỆP VỤ HỦY ĐƠN (CANCELED)
        if (newStatus == OrderStatus.CANCELED) {
            if (oldStatus == OrderStatus.DELIVERED) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }

            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getVariant();
                Inventory inventory = inventoryRepository.findByVariantId(variant.getId()).orElse(null);
                if (inventory != null) {
                    if (inventory.getVersion() == null) inventory.setVersion(0L);

//                    inventory.setReserved(inventory.getReserved() - item.getQuantity());
                    inventory.setReserved(Math.max(0, inventory.getReserved() - item.getQuantity()));

                    inventoryRepository.save(inventory);

                    inventoryLogRepository.save(InventoryLog.builder()
                            .variantId(variant.getId())
                            .changeQuantity(item.getQuantity())
                            .changeType(StockChangeType.RELEASE)
                            .note("Admin hủy đơn hàng: " + order.getOrderNumber() + ", nhả giữ chỗ")
                            .performedByUserId(adminUserId)
                            .build());
                }
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
        }

        // 4. NGHIỆP VỤ GIAO THÀNH CÔNG (DELIVERED)
        else if (newStatus == OrderStatus.DELIVERED) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getVariant();
                Inventory inventory = inventoryRepository.findByVariantId(variant.getId()).orElse(null);
                if (inventory != null) {
                    if (inventory.getVersion() == null) inventory.setVersion(0L);

//                    inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
//                    inventory.setReserved(inventory.getReserved() - item.getQuantity());
                    inventory.setQuantity(Math.max(0, inventory.getQuantity() - item.getQuantity()));
                    inventory.setReserved(Math.max(0, inventory.getReserved() - item.getQuantity()));

                    inventoryRepository.save(inventory);

                    inventoryLogRepository.save(InventoryLog.builder()
                            .variantId(variant.getId())
                            .changeQuantity(-item.getQuantity())
                            .changeType(StockChangeType.SALE)
                            .note("Giao thành công đơn: " + order.getOrderNumber() + ", xuất bán")
                            .performedByUserId(adminUserId)
                            .build());

                    Product product = variant.getProduct();
                    product.setSoldCount(product.getSoldCount() + item.getQuantity());
                    productRepository.save(product);
                }
            }
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        // 5. NGHIỆP VỤ TRẢ HÀNG (REFUNDED)
        else if (newStatus == OrderStatus.REFUNDED) {
            if (oldStatus != OrderStatus.DELIVERED) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }

            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getVariant();
                Inventory inventory = inventoryRepository.findByVariantId(variant.getId()).orElse(null);
                if (inventory != null) {
                    if (inventory.getVersion() == null) inventory.setVersion(0L);

                    inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
                    inventoryRepository.save(inventory);

                    inventoryLogRepository.save(InventoryLog.builder()
                            .variantId(variant.getId())
                            .changeQuantity(item.getQuantity())
                            .changeType(StockChangeType.RETURN)
                            .note("Khách hoàn trả đơn: " + order.getOrderNumber() + ", nhập lại kho")
                            .performedByUserId(adminUserId)
                            .build());

                    Product product = variant.getProduct();
                    long newSoldCount = product.getSoldCount() - item.getQuantity();
                    product.setSoldCount(Math.max(0, newSoldCount)); // Đảm bảo không bị âm
                    productRepository.save(product);
                }
            }

            List<Review> orderReviews = reviewRepository.findByOrderId(orderId);
            Set<Product> affectedProducts = new HashSet<>(); // Dùng Set để lọc các sản phẩm bị ảnh hưởng

            for (Review review : orderReviews) {
                review.setApproved(false);
                reviewRepository.save(review);
                affectedProducts.add(review.getProduct());
            }

            // Tính toán và lưu cứng lại số sao mới
            for (Product p : affectedProducts) {
                long count = reviewRepository.countApprovedReviewsByProductId(p.getId());
                Double avg = reviewRepository.getAverageRatingByProductId(p.getId());
                p.setReviewCount(count);
                p.setRatingAvg(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
                productRepository.save(p);
            }

            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    //  LẤY CHI TIẾT 1 ĐƠN HÀNG CHO ADMIN
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetailForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Map danh sách sản phẩm
        List<Map<String, Object>> items = order.getItems().stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("productName", item.getProductNameSnapshot());
            map.put("sku", item.getSkuSnapshot());
            map.put("color", item.getColorSnapshot());
            map.put("size", item.getSizeSnapshot());
            map.put("imageUrl", item.getImageUrlSnapshot());
            map.put("unitPrice", item.getUnitPriceSnapshot());
            map.put("quantity", item.getQuantity());
            map.put("lineTotal", item.getLineTotal());
            return map;
        }).toList();

        // Gói gọn thông tin trả về
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNumber", order.getOrderNumber());
        result.put("status", order.getStatus());
        result.put("paymentMethod", order.getPaymentMethod());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("totalAmount", order.getTotalAmount());
        result.put("shippingFee", order.getShippingFee());
        result.put("discount", order.getDiscount());
        result.put("createdAt", order.getCreatedAt());

        // Thông tin địa chỉ từ Snapshot
        if (order.getShippingAddress() != null) {
            result.put("receiverName", order.getShippingAddress().getReceiverName());
            result.put("phone", order.getShippingAddress().getPhone());
            result.put("fullAddress", order.getShippingAddress().getStreet() + ", " +
                    order.getShippingAddress().getWard() + ", " +
                    order.getShippingAddress().getDistrict() + ", " +
                    order.getShippingAddress().getProvince());
            result.put("note", order.getShippingAddress().getNote());
        }

        result.put("items", items);
        return result;
    }

    // LẤY DANH SÁCH ĐƠN HÀNG CỦA TÔI (Có lọc ngày tháng và Danh sách sản phẩm)
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyOrders(int page, int size, String keyword, OrderStatus status, PaymentStatus paymentStatus, String startDateStr, String endDateStr) {
        String userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        // Xử lý chuyển đổi chuỗi ngày (YYYY-MM-DD) sang Instant chuẩn Giờ Việt Nam
        Instant startDate = null;
        Instant endDate = null;
        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            }
        } catch (Exception e) {
            // Bỏ qua lỗi parse ngày nếu frontend gửi sai định dạng
        }

        Page<Order> orderPage = orderRepository.searchMyOrders(userId, keyword, status, paymentStatus, startDate, endDate, pageable);

        return orderPage.map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNumber", order.getOrderNumber());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus());
            map.put("paymentMethod", order.getPaymentMethod());
            map.put("paymentStatus", order.getPaymentStatus());
            map.put("createdAt", order.getCreatedAt());

            // ĐÃ THÊM: Gói danh sách sản phẩm để hiển thị trực tiếp ra Thẻ (Card)
            List<Map<String, Object>> items = order.getItems().stream().map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", item.getVariant().getProduct().getId());
                itemMap.put("productSlug", item.getVariant().getProduct().getSlug()); // Để click vào nhảy tới trang chi tiết
                itemMap.put("productName", item.getProductNameSnapshot());
                itemMap.put("color", item.getColorSnapshot());
                itemMap.put("size", item.getSizeSnapshot());
                itemMap.put("imageUrl", item.getImageUrlSnapshot());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPriceSnapshot());
                itemMap.put("lineTotal", item.getLineTotal());
                return itemMap;
            }).toList();

            map.put("items", items);
            return map;
        });
    }

    // --- 5. LẤY CHI TIẾT 1 ĐƠN HÀNG (DÀNH CHO KHÁCH) ---
    @Transactional(readOnly = true)
    public Map<String, Object> getMyOrderDetail(Long orderId) {
        String userId = getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Rào chắn bảo mật: Không cho phép xem trộm đơn của người khác
        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Logic map dữ liệu y hệt như hàm getOrderDetailForAdmin
        List<Map<String, Object>> items = order.getItems().stream().map(item -> {
            Map<String, Object> map = new HashMap<>();

            Long productId = item.getVariant().getProduct().getId();

            map.put("id", item.getId());
            map.put("productId", item.getVariant().getProduct().getId());
            map.put("productName", item.getProductNameSnapshot());
            map.put("productSlug", item.getVariant().getProduct().getSlug());
            map.put("color", item.getColorSnapshot());
            map.put("size", item.getSizeSnapshot());
            map.put("imageUrl", item.getImageUrlSnapshot());
            map.put("unitPrice", item.getUnitPriceSnapshot());
            map.put("quantity", item.getQuantity());
            map.put("lineTotal", item.getLineTotal());

            // ĐÃ THÊM: Kiểm tra xem User đã đánh giá món này trong đơn này chưa
            boolean isReviewed = reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, orderId, productId);
            map.put("isReviewed", isReviewed);

            return map;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNumber", order.getOrderNumber());
        result.put("status", order.getStatus());
        result.put("paymentMethod", order.getPaymentMethod());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("totalAmount", order.getTotalAmount());
        result.put("shippingFee", order.getShippingFee());
        result.put("discount", order.getDiscount());
        result.put("createdAt", order.getCreatedAt());

        if (order.getShippingAddress() != null) {
            result.put("receiverName", order.getShippingAddress().getReceiverName());
            result.put("phone", order.getShippingAddress().getPhone());
            result.put("fullAddress", order.getShippingAddress().getStreet() + ", " + order.getShippingAddress().getWard() + ", " + order.getShippingAddress().getDistrict() + ", " + order.getShippingAddress().getProvince());
            result.put("note", order.getShippingAddress().getNote());
        }
        result.put("items", items);
        return result;
    }


    // KHÁCH HÀNG TỰ HỦY ĐƠN ---
    @Transactional
    public void cancelMyOrder(Long orderId) {
        String userId = getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Rào chắn 1: Đảm bảo đơn này là của đúng user đang đăng nhập
        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Rào chắn 2: Chỉ cho phép tự hủy khi đơn đang PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // Tiến hành hủy đơn và nhả kho
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            Inventory inventory = inventoryRepository.findByVariantId(variant.getId()).orElse(null);
            if (inventory != null) {
                if (inventory.getVersion() == null) inventory.setVersion(0L);
//                inventory.setReserved(inventory.getReserved() - item.getQuantity());
                inventory.setReserved(Math.max(0, inventory.getReserved() - item.getQuantity()));

                inventoryRepository.save(inventory);

                inventoryLogRepository.save(InventoryLog.builder()
                        .variantId(variant.getId())
                        .changeQuantity(item.getQuantity())
                        .changeType(StockChangeType.RELEASE)
                        .note("Khách tự hủy đơn hàng: " + order.getOrderNumber())
                        .performedByUserId(userId)
                        .build());
            }
        }

        order.setStatus(OrderStatus.CANCELED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }
        orderRepository.save(order);
    }

    // --- 6. MUA LẠI ĐƠN HÀNG CŨ ---
    @Transactional
    public void repurchaseOrder(Long orderId) {
        String userId = getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lặp qua các món trong đơn cũ
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();

            // Chỉ thêm vào giỏ nếu Phân loại đó và Sản phẩm gốc vẫn còn đang kinh doanh (Active)
            if (variant.getActive() != null && variant.getActive() && variant.getProduct().isActive()) {

                // Kiểm tra xem trong giỏ hiện tại đã có món này chưa
                CartItem existingItem = cart.getItems().stream()
                        .filter(ci -> ci.getVariant().getId().equals(variant.getId()))
                        .findFirst()
                        .orElse(null);

                if (existingItem != null) {
                    existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                    cartItemRepository.save(existingItem);
                } else {
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .variant(variant)
                            .quantity(item.getQuantity())
                            .build();
                    cartItemRepository.save(newItem);
                    cart.getItems().add(newItem);
                }
            }
        }
        cartRepository.save(cart);
    }
}