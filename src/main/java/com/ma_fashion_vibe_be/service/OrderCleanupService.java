package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.entities.*;
import com.ma_fashion_vibe_be.enums.OrderStatus;
import com.ma_fashion_vibe_be.enums.PaymentMethod;
import com.ma_fashion_vibe_be.enums.PaymentStatus;
import com.ma_fashion_vibe_be.enums.StockChangeType;
import com.ma_fashion_vibe_be.repository.InventoryLogRepository;
import com.ma_fashion_vibe_be.repository.InventoryRepository;
import com.ma_fashion_vibe_be.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderCleanupService {

    OrderRepository orderRepository;
    InventoryRepository inventoryRepository;
    InventoryLogRepository inventoryLogRepository;

    // Chạy tự động mỗi 60 giây (60000 ms)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupUnpaidVNPayOrders() {
        // Lấy mốc thời gian cách đây 25 phút, dư hơn 10p tránh trường hợp thanh toán ngân hàng bị chậm
        Instant timeLimit = Instant.now().minus(25, ChronoUnit.MINUTES);

        List<Order> expiredOrders = orderRepository.findExpiredUnpaidOrders(
                PaymentMethod.VNPAY,
                PaymentStatus.UNPAID,
                OrderStatus.PENDING,
                timeLimit
        );

        if (!expiredOrders.isEmpty()) {
            log.info("Phát hiện {} đơn hàng VNPAY quá hạn chưa thanh toán. Đang tiến hành hủy và nhả kho...", expiredOrders.size());
        }

        for (Order order : expiredOrders) {
            // 1. Chuyển trạng thái đơn thành CANCELED và FAILED
            order.setStatus(OrderStatus.CANCELED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            // 2. Nhả tồn kho (Release Reserved)
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getVariant();
                Inventory inventory = inventoryRepository.findByVariantId(variant.getId()).orElse(null);

                if (inventory != null) {
//                    inventory.setReserved(inventory.getReserved() - item.getQuantity());
                    inventory.setReserved(Math.max(0, inventory.getReserved() - item.getQuantity()));

                    inventoryRepository.save(inventory);

                    // Ghi log hệ thống tự thu hồi
                    InventoryLog invLog = InventoryLog.builder()
                            .variantId(variant.getId())
                            .changeQuantity(item.getQuantity())
                            .changeType(StockChangeType.RELEASE)
                            .note("Hệ thống tự động nhả kho do đơn hàng " + order.getOrderNumber() + " quá hạn thanh toán")
                            .performedByUserId("SYSTEM") // Hệ thống tự làm
                            .build();
                    inventoryLogRepository.save(invLog);
                }
            }
            log.info("Đã hủy thành công đơn hàng: {}", order.getOrderNumber());
        }
    }
}