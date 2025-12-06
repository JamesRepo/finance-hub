package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find all categories for a user (including system categories)
     */
    @Query("SELECT c FROM Category c WHERE c.user = :user OR c.isSystem = true ORDER BY c.categoryName")
    List<Category> findByUserOrSystem(@Param("user") User user);

    /**
     * Find categories by type for a user (including system categories)
     */
    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.isSystem = true) AND c.categoryType = :type ORDER BY c.categoryName")
    List<Category> findByUserOrSystemAndType(@Param("user") User user, @Param("type") CategoryType type);

    /**
     * Count user-specific categories (excluding system categories)
     */
    long countByUser(User user);

    /**
     * Find categories by type
     */
    List<Category> findByCategoryType(CategoryType categoryType);

    /**
     * Find a category by name (case-insensitive) for a user
     * Also searches system categories (user_id is null)
     */
    @Query("SELECT c FROM Category c " +
           "WHERE LOWER(c.categoryName) = LOWER(:categoryName) " +
           "AND (c.user = :user OR c.isSystem = true)")
    Optional<Category> findByCategoryNameIgnoreCaseAndUser(
            @Param("categoryName") String categoryName,
            @Param("user") User user
    );
}