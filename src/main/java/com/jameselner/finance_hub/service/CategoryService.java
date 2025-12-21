package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.dto.CategoryDTO;
import com.jameselner.finance_hub.mapper.CategoryMapper;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Create a new category from DTO
     */
    @Transactional
    public CategoryDTO createFromDto(final CategoryDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getCategoryId() != null) {
            throw new IllegalArgumentException("Category ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);

        Category category = categoryMapper.toEntity(dto);

        // Set defaults if not provided
        if (category.getIsSystem() == null) {
            category.setIsSystem(false);
        }

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    /**
     * Update an existing category from DTO
     */
    @Transactional
    public CategoryDTO updateFromDto(final CategoryDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID must not be null for update operation");
        }

        Category existingCategory = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category with ID " + dto.getCategoryId() + " does not exist"));

        // Prevent updating system categories
        if (existingCategory.getIsSystem()) {
            throw new IllegalArgumentException("Cannot update system categories");
        }

        validateDtoRequiredFields(dto);

        categoryMapper.updateEntityFromDto(existingCategory, dto);
        Category savedCategory = categoryRepository.save(existingCategory);

        return categoryMapper.toDto(savedCategory);
    }

    /**
     * Find category by ID as DTO
     */
    public Optional<CategoryDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto);
    }

    /**
     * Find all categories for a user (including system categories)
     */
    public List<CategoryDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return categoryRepository.findByUserOrSystem(user).stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find categories by type for a user (including system categories)
     */
    public List<CategoryDTO> findByUserIdAndTypeAsDto(final Long userId, final CategoryType type) {
        validateIdNotNull(userId);
        if (type == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        User user = getUserById(userId);

        return categoryRepository.findByUserOrSystemAndType(user, type).stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete category by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category with ID " + id + " does not exist"));

        // Prevent deleting system categories
        if (category.getIsSystem()) {
            throw new IllegalArgumentException("Cannot delete system categories");
        }

        categoryRepository.deleteById(id);
    }

    /**
     * Count user-specific categories (excluding system categories)
     */
    public long countByUserId(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return categoryRepository.countByUser(user);
    }

    // Legacy methods - still used by TransactionForm, BudgetForm, TransactionView
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public List<Category> findByType(final CategoryType type) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getCategoryType() == type)
                .collect(Collectors.toList());
    }

    public Optional<Category> findById(final Long id) {
        return categoryRepository.findById(id);
    }

    // Private helper methods

    private User getUserById(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    private void validateDtoNotNull(final CategoryDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CategoryDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final CategoryDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name must not be null or empty");
        }
        if (dto.getCategoryType() == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
    }
}