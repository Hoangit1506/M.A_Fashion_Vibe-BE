package com.ma_fashion_vibe_be.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1009, "Tài khoản Email đã tồn tại!", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1010, "Mật khẩu không đúng!", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_EXPIRED(1011, "Refresh token expired", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(1012, "Mật khẩu xác nhận không khớp!", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1013, "Mã OTP không hợp lệ hoặc đã hết hạn!", HttpStatus.BAD_REQUEST),
    CANNOT_LOCK_YOURSELF(1014, "Bạn không thể tự khóa tài khoản của chính mình!", HttpStatus.BAD_REQUEST),
    CANNOT_LOCK_ADMIN(1015, "Bạn không thể khóa tài khoản của Quản trị viên khác!", HttpStatus.FORBIDDEN),
    USER_DISABLED(1016, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên!", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN(1017, "Refresh token không hợp lệ hoặc đã bị thu hồi!", HttpStatus.UNAUTHORIZED),
    TOKEN_HASH_ERROR(1018, "Lỗi mã hóa token hệ thống!", HttpStatus.INTERNAL_SERVER_ERROR),

    CATEGORY_NOT_FOUND(2001, "Danh mục không tồn tại hoặc không hợp lệ!", HttpStatus.BAD_REQUEST),
    CATEGORY_DUPLICATE_SLUG(2002, "Tên danh mục này tạo ra đường dẫn bị trùng lặp!", HttpStatus.BAD_REQUEST),
    CATEGORY_DUPLICATE_SORT(2003, "Thứ tự hiển thị đã bị trùng với một danh mục khác cùng cấp!", HttpStatus.BAD_REQUEST),
    CATEGORY_PARENT_NOT_FOUND(2004, "Danh mục cha không tồn tại hoặc không hợp lệ!", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_CHILDREN(2005, "Không thể xóa vì đang chứa danh mục con!", HttpStatus.BAD_REQUEST),
    CATEGORY_CAN_NOT_BE_ITS_PARENT(2006, "Danh mục không thể tự làm cha của chính nó!", HttpStatus.BAD_REQUEST),
    PRODUCT_DUPLICATE_SLUG(2007, "Đường dẫn sản phẩm đã tồn tại!", HttpStatus.BAD_REQUEST),
    VARIANT_DUPLICATE_SKU(2008, "Mã SKU phân loại hàng đã tồn tại!", HttpStatus.BAD_REQUEST),
    FILE_EMPTY(2009, "File upload không được để trống!", HttpStatus.BAD_REQUEST),
    UPLOAD_IMAGE_FAILED(2010, "Lỗi khi tải ảnh lên Cloudinary!", HttpStatus.INTERNAL_SERVER_ERROR),
    DELETE_IMAGE_FAILED(2011, "Lỗi khi xóa ảnh trên Cloudinary!", HttpStatus.INTERNAL_SERVER_ERROR),
    PRODUCT_NOT_FOUND(2012, "Sản phẩm không tồn tại!", HttpStatus.BAD_REQUEST),
    IMAGE_SIZE_EXCEEDED(2013, "Dung lượng ảnh tối đa là 10MB!", HttpStatus.BAD_REQUEST),
    VIDEO_SIZE_EXCEEDED(2014, "Dung lượng video tối đa là 100MB!", HttpStatus.BAD_REQUEST),
    INVALID_MEDIA_TYPE(2015, "Định dạng file không hợp lệ! Phân loại hàng chỉ chấp nhận ảnh.", HttpStatus.BAD_REQUEST),
    PRODUCT_INACTIVE(2016, "Sản phẩm hoặc phân loại hàng này đã ngừng kinh doanh!", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(2017, "Số lượng tồn kho không đủ để thực hiện giao dịch!", HttpStatus.BAD_REQUEST),
    OPTIMISTIC_LOCKING_CONFLICT(2018, "Sản phẩm bạn chọn vừa có người cập nhật hoặc mua mất. Vui lòng tải lại trang!", HttpStatus.CONFLICT),
    DATA_BINDING_VIOLATION(2019, "Ngoại lệ vi phạm tính toàn vẹn dữ liệu!", HttpStatus.BAD_REQUEST),
    PRODUCT_IN_USE(2020, "Sản phẩm đã phát sinh giao dịch. Chỉ có thể dùng công tắc Tắt hiển thị!", HttpStatus.BAD_REQUEST),
    VARIANT_IN_USE(2021, "Phân loại hàng này đã phát sinh giao dịch, không thể xóa cứng. Vui lòng gạt công tắc Tắt hiển thị thay vì bấm nút Xóa!", HttpStatus.BAD_REQUEST),
    COVER_IMAGE_REQUIRED(2022, "Ảnh đại diện (vị trí đầu tiên) bắt buộc phải là Hình ảnh, không được là Video!", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(2023, "Đơn hàng không tồn tại trên hệ thống", HttpStatus.NOT_FOUND),
    INVALID_ORDER_STATUS(2024, "Chuyển đổi trạng thái đơn hàng không hợp lệ", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL(2025, "Chỉ có thể hủy đơn hàng khi đang ở trạng thái Chờ xác nhận", HttpStatus.BAD_REQUEST),
    NOT_ELIGIBLE_FOR_REVIEW(2026, "Bạn không đủ điều kiện để đánh giá sản phẩm này", HttpStatus.FORBIDDEN),
    REVIEW_ALREADY_EXISTS(2027, "Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi", HttpStatus.BAD_REQUEST),
    ADDRESS_NOT_FOUND(2028, "Địa chỉ không tồn tại!", HttpStatus.NOT_FOUND),
    ADDRESS_ACCESS_DENIED(2029, "Bạn không có quyền thao tác trên địa chỉ này!", HttpStatus.FORBIDDEN),
    ADDRESS_DEFAULT_CANNOT_DELETE(2030, "Không thể xóa địa chỉ mặc định!", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_FOUND(2031, "Đánh giá không tồn tại hoặc đã bị ẩn/xóa!", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND(2032, "Giỏ hàng của người dùng không tồn tại!", HttpStatus.NOT_FOUND),
    CART_EMPTY(2033, "Giỏ hàng của bạn đang trống, không thể đặt hàng!", HttpStatus.BAD_REQUEST),
    ADDRESS_INVALID(2034, "Địa chỉ giao hàng không hợp lệ!", HttpStatus.BAD_REQUEST),
    ORDER_ITEM_NOT_FOUND(2035, "Không tìm thấy chi tiết món hàng trong đơn!", HttpStatus.NOT_FOUND),
    INVENTORY_NOT_FOUND(2036, "Không tìm thấy thông tin kho hàng cho sản phẩm này!", HttpStatus.NOT_FOUND),
    CATEGORY_CIRCULAR_REFERENCE(2037, "Lỗi vòng lặp: Không thể chọn danh mục con/cháu làm danh mục cha!", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
