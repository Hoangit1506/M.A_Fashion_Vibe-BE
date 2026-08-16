package com.ma_fashion_vibe_be.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtils {
    public static String toSlug(String input) {
        if (input == null) return "";

        // Chuyển tiếng Việt có dấu thành không dấu
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        // Chuyển thành chữ thường, thay khoảng trắng bằng dấu gạch ngang
        return slug.toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "") // Xóa ký tự đặc biệt
                .replaceAll("\\s+", "-")         // Thay dấu cách bằng -
                .replaceAll("-+", "-");          // Xóa các dấu - liên tiếp
    }
}