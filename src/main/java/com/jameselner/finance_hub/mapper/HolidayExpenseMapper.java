package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.HolidayExpense;
import com.jameselner.finance_hub.dto.HolidayExpenseDTO;
import com.jameselner.finance_hub.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HolidayExpenseMapper {

    private final HolidayRepository holidayRepository;

    public HolidayExpenseDTO toDto(final HolidayExpense expense) {
        if (expense == null) {
            return null;
        }

        return HolidayExpenseDTO.builder()
                .expenseId(expense.getExpenseId())
                .holidayId(expense.getHoliday() != null ? expense.getHoliday().getHolidayId() : null)
                .holidayName(expense.getHoliday() != null ? expense.getHoliday().getName() : null)
                .expenseType(expense.getExpenseType())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    public HolidayExpense toEntity(final HolidayExpenseDTO dto) {
        if (dto == null) {
            return null;
        }

        HolidayExpense expense = new HolidayExpense();
        updateEntityFromDto(expense, dto);
        return expense;
    }

    public void updateEntityFromDto(final HolidayExpense expense, final HolidayExpenseDTO dto) {
        if (expense == null || dto == null) {
            return;
        }

        if (dto.getHolidayId() != null) {
            Holiday holiday = holidayRepository.findById(dto.getHolidayId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Holiday with ID " + dto.getHolidayId() + " not found"));
            expense.setHoliday(holiday);
        }

        expense.setExpenseType(dto.getExpenseType());
        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setNotes(dto.getNotes());
    }
}