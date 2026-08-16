package com.ma_fashion_vibe_be.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReviewReplyRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    String replyContent;
}