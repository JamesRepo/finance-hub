package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Budget;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BudgetMapper {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Convert Budget entity to BudgetDTO (without calculated fields)
     * Calculated fields (spent, remaining, overage, percentageUsed) should be set by the service layer
     */
    public BudgetDTO toDto(final Budget budget) {
        if (budget == null) {
            return null;
        }

        return BudgetDTO.builder()
                .budgetId(budget.getBudgetId())
                .userId(budget.getUser() != null ? budget.getUser().getUserId() : null)
                .userEmail(budget.getUser() != null ? budget.getUser().getEmail() : null)
                .categoryId(budget.getCategory() != null ? budget.getCategory().getCategoryId() : null)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getCategoryName() : null)
                .categoryColorCode(budget.getCategory() != null ? budget.getCategory().getColorCode() : null)
                .amount(budget.getAmount())
                .periodType(budget.getPeriodType())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                // Calculated fields will be set by service layer
                .spent(BigDecimal.ZERO)
                .remaining(budget.getAmount())
                .overage(BigDecimal.ZERO)
                .percentageUsed(0.0)
                .build();
    }

    /**
     * Convert BudgetDTO to Budget entity (for create operations)
     * This method fetches the User and Category entities from the database
     */
    public Budget toEntity(final BudgetDTO dto) {
        if (dto == null) {
            return null;
        }

        Budget budget = new Budget();
        updateEntityFromDto(budget, dto);
        return budget;
    }

    /**
     * Update an existing Budget entity from a BudgetDTO
     * This method fetches the User and Category entities from the database
     */
    public void updateEntityFromDto(final Budget budget, final BudgetDTO dto) {
        if (budget == null || dto == null) {
            throw new IllegalArgumentException("Budget and DTO must not be null");
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + dto.getUserId()));
            budget.setUser(user);
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dto.getCategoryId()));
            budget.setCategory(category);
        }

        budget.setAmount(dto.getAmount());
        budget.setPeriodType(dto.getPeriodType());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
    }
}
