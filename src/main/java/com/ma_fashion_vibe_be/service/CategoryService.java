package com.ma_fashion_vibe_be.service;

import com.ma_fashion_vibe_be.dto.PageResponse;
import com.ma_fashion_vibe_be.dto.category.CategoryRequest;
import com.ma_fashion_vibe_be.dto.category.CategoryResponse;
import com.ma_fashion_vibe_be.dto.category.CategoryUpdateRequest;
import com.ma_fashion_vibe_be.entities.Category;
import com.ma_fashion_vibe_be.exception.AppException;
import com.ma_fashion_vibe_be.exception.ErrorCode;
import com.ma_fashion_vibe_be.mapper.CategoryMapper;
import com.ma_fashion_vibe_be.repository.CategoryRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    // 1. Lấy toàn bộ cây danh mục (Dùng cho Frontend hiển thị Menu)
    public List<CategoryResponse> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        return rootCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Hàm đệ quy: Biến Entity thành DTO và lặp lại với các thẻ con
    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = categoryMapper.toCategoryResponse(category);

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<CategoryResponse> childrenDTO = category.getChildren().stream()
                    .filter(Category::isActive)
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            response.setChildren(childrenDTO);
        }
        return response;
    }

    // 2. Lấy danh sách phân trang và tìm kiếm (Dành cho Admin Table)
    public PageResponse<CategoryResponse> getCategoriesWithPaginationAndSearch(
            int page, int size, String keyword, Long filterParentId, String sortBy, String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Gọi hàm Query thông minh vừa tạo ở Repository
        Page<Category> categoryPage = categoryRepository.findCategoriesAdmin(keyword, filterParentId, pageable);

        List<CategoryResponse> dtoList = categoryPage.getContent().stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());

        return PageResponse.<CategoryResponse>builder()
                .currentPage(page)
                .totalPages(categoryPage.getTotalPages())
                .pageSize(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    // 3. HÀM CHẶN TRÙNG THỨ TỰ (Validate)
    private void validateSortOrderUniqueness(Long parentId, Integer sortOrder, Long currentId) {
        Long idToCheck = currentId == null ? 0L : currentId;
        boolean isDuplicate;

        if (parentId != null) {
            isDuplicate = categoryRepository.existsByParentIdAndSortOrderAndIdNot(parentId, sortOrder, idToCheck);
        } else {
            isDuplicate = categoryRepository.existsByParentIsNullAndSortOrderAndIdNot(sortOrder, idToCheck);
        }

        if (isDuplicate) {
            throw new AppException(ErrorCode.CATEGORY_DUPLICATE_SORT);
        }
    }

    // 4. Thêm mới danh mục
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // [QUAN TRỌNG] Đã thêm: Kiểm tra trùng sortOrder trước khi tạo
        validateSortOrderUniqueness(request.getParentId(), request.getSortOrder(), null);

        Category category = categoryMapper.toCategory(request);
        category.setActive(true);

        String slug = StringUtils.toSlug(request.getName());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));
            category.setParent(parent);
        }

        String baseSlug = StringUtils.toSlug(request.getName());
        String autoSlug = category.getParent() != null ? category.getParent().getSlug() + "-" + baseSlug : baseSlug;

        if (categoryRepository.existsBySlug(autoSlug)) {
            throw new AppException(ErrorCode.CATEGORY_DUPLICATE_SLUG);
        }
        category.setSlug(autoSlug);

        category = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(category);
    }

    // 5. Cập nhật danh mục
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        // [QUAN TRỌNG] Kiểm tra trùng sortOrder trước khi cập nhật
        validateSortOrderUniqueness(request.getParentId(), request.getSortOrder(), id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

//        if (request.getParentId() != null) {
//            if (request.getParentId().equals(id)) {
//                throw new AppException(ErrorCode.CATEGORY_CAN_NOT_BE_ITS_PARENT);
//            }
//            Category parent = categoryRepository.findById(request.getParentId())
//                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));
//            category.setParent(parent);
//        } else {
//            category.setParent(null);
//        }


        if (request.getParentId() != null) {
            // 1. BẮT LỖI CHỌN CHÍNH NÓ LÀM DANH MỤC CHA
            if (request.getParentId().equals(id)) {
                throw new AppException(ErrorCode.CATEGORY_CAN_NOT_BE_ITS_PARENT);
            }

            // 2. BẮT LỖI VÒNG LẶP SÂU (Chống cháu chắt ngược lên làm cha)
            Long checkParentId = request.getParentId();
            while (checkParentId != null) {
                if (checkParentId.equals(id)) {
                    throw new AppException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE);
                }
                Category tempParent = categoryRepository.findById(checkParentId).orElse(null);
                checkParentId = (tempParent != null && tempParent.getParent() != null)
                        ? tempParent.getParent().getId()
                        : null;
            }

            // 3. Vượt qua 2 lớp kiểm tra an toàn thì mới gán Parent
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }


        String baseSlug = StringUtils.toSlug(request.getName());
        String autoSlug = category.getParent() != null ? category.getParent().getSlug() + "-" + baseSlug : baseSlug;

        if (!category.getSlug().equals(autoSlug) && categoryRepository.existsBySlug(autoSlug)) {
            throw new AppException(ErrorCode.CATEGORY_DUPLICATE_SLUG);
        }
        category.setSlug(autoSlug);

        category = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(category);
    }

    // 6. Xóa danh mục
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        categoryRepository.delete(category);
    }
}