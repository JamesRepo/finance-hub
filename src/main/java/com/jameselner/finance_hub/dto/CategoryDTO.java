package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long categoryId;
    private Long userId;
    private String userEmail;
    private String categoryName;
    private CategoryType categoryType;
    private String colorCode;
    private Boolean isSystem;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
