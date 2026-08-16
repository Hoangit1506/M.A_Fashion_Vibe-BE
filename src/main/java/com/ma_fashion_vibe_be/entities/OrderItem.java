package com.ma_fashion_vibe_be.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id")
})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    ProductVariant variant;

    // snapshot fields
    @Column(name = "product_name_snapshot", length = 255)
    String productNameSnapshot;

    @Column(name = "sku_snapshot", length = 100)
    String skuSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    String imageUrlSnapshot;

    @Column(name = "color_snapshot", length = 50)
    String colorSnapshot;

    @Column(name = "size_snapshot", length = 50)
    String sizeSnapshot;

    @Column(name = "unit_price_snapshot", precision = 13, scale = 2)
    BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "line_total", precision = 13, scale = 2)
    BigDecimal lineTotal;
}
