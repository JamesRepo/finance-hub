package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.CategoryDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final UserRepository userRepository;

    public CategoryDTO toDto(final Category category) {
        if (category == null) {
            return null;
        }

        return CategoryDTO.builder()
                .categoryId(category.getCategoryId())
                .userId(category.getUser() != null ? category.getUser().getUserId() : null)
                .userEmail(category.getUser() != null ? category.getUser().getEmail() : null)
                .categoryName(category.getCategoryName())
                .categoryType(category.getCategoryType())
                .colorCode(category.getColorCode())
                .isSystem(category.getIsSystem())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public Category toEntity(final CategoryDTO dto) {
        if (dto == null) {
            return null;
        }

        Category category = new Category();
        updateEntityFromDto(category, dto);
        return category;
    }

    public void updateEntityFromDto(final Category category, final CategoryDTO dto) {
        if (category == null || dto == null) {
            return;
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User with ID " + dto.getUserId() + " not found"));
            category.setUser(user);
        }

        category.setCategoryName(dto.getCategoryName());
        category.setCategoryType(dto.getCategoryType());
        category.setColorCode(dto.getColorCode());

        if (dto.getIsSystem() != null) {
            category.setIsSystem(dto.getIsSystem());
        }
    }
}
