package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.HousingExpense;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.HousingExpenseDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HousingExpenseMapper {

    private final UserRepository userRepository;

    public HousingExpenseDTO toDto(final HousingExpense expense) {
        if (expense == null) {
            return null;
        }

        return HousingExpenseDTO.builder()
                .expenseId(expense.getExpenseId())
                .userId(expense.getUser() != null ? expense.getUser().getUserId() : null)
                .userEmail(expense.getUser() != null ? expense.getUser().getEmail() : null)
                .expenseType(expense.getExpenseType())
                .amount(expense.getAmount())
                .expenseMonth(expense.getExpenseMonth())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    public HousingExpense toEntity(final HousingExpenseDTO dto) {
        if (dto == null) {
            return null;
        }

        HousingExpense expense = new HousingExpense();
        updateEntityFromDto(expense, dto);
        return expense;
    }

    public void updateEntityFromDto(final HousingExpense expense, final HousingExpenseDTO dto) {
        if (expense == null || dto == null) {
            return;
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User with ID " + dto.getUserId() + " not found"));
            expense.setUser(user);
        }

        expense.setExpenseType(dto.getExpenseType());
        expense.setAmount(dto.getAmount());
        expense.setExpenseMonth(dto.getExpenseMonth());
    }
}
