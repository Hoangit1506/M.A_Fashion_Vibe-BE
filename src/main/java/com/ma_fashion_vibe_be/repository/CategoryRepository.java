package com.ma_fashion_vibe_be.repository;

import com.ma_fashion_vibe_be.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentId(Long parentId);
    // Tìm tất cả danh mục gốc (không có cha) để làm điểm bắt đầu vẽ cây
    List<Category> findByParentIsNullOrderBySortOrderAsc();

    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    // Câu lệnh Query thông minh: Hỗ trợ tìm theo tên VÀ tìm theo danh mục cha
    // Quy ước: parentId = 0 nghĩa là tìm các Danh mục gốc (Nữ, Nam)
    @Query("SELECT c FROM Category c WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:parentId IS NULL OR " +
            "(:parentId = 0 AND c.parent IS NULL) OR " +
            "(c.parent IS NOT NULL AND c.parent.id = :parentId))")
    Page<Category> findCategoriesAdmin(
            @Param("keyword") String keyword,
            @Param("parentId") Long parentId,
            Pageable pageable);

    // Dùng để chặn trùng sortOrder trong cùng 1 cấp
    boolean existsByParentIdAndSortOrderAndIdNot(Long parentId, Integer sortOrder, Long id);
    boolean existsByParentIsNullAndSortOrderAndIdNot(Integer sortOrder, Long id);
}
