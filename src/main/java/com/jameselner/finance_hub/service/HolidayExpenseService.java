package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.HolidayExpense;
import com.jameselner.finance_hub.domain.enums.HolidayExpenseType;
import com.jameselner.finance_hub.dto.HolidayExpenseDTO;
import com.jameselner.finance_hub.mapper.HolidayExpenseMapper;
import com.jameselner.finance_hub.repository.HolidayExpenseRepository;
import com.jameselner.finance_hub.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayExpenseService {

    private final HolidayExpenseRepository holidayExpenseRepository;
    private final HolidayRepository holidayRepository;
    private final HolidayExpenseMapper holidayExpenseMapper;

    /**
     * Create a new holiday expense from DTO
     */
    @Transactional
    public HolidayExpenseDTO createFromDto(final HolidayExpenseDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getExpenseId() != null) {
            throw new IllegalArgumentException("Expense ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto);

        HolidayExpense expense = holidayExpenseMapper.toEntity(dto);
        HolidayExpense savedExpense = holidayExpenseRepository.save(expense);

        return holidayExpenseMapper.toDto(savedExpense);
    }

    /**
     * Update an existing holiday expense from DTO
     */
    @Transactional
    public HolidayExpenseDTO updateFromDto(final HolidayExpenseDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getExpenseId() == null) {
            throw new IllegalArgumentException("Expense ID must not be null for update operation");
        }

        HolidayExpense existingExpense = holidayExpenseRepository.findById(dto.getExpenseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Holiday expense with ID " + dto.getExpenseId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto);

        holidayExpenseMapper.updateEntityFromDto(existingExpense, dto);
        HolidayExpense savedExpense = holidayExpenseRepository.save(existingExpense);

        return holidayExpenseMapper.toDto(savedExpense);
    }

    /**
     * Find holiday expense by ID as DTO
     */
    public Optional<HolidayExpenseDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return holidayExpenseRepository.findById(id)
                .map(holidayExpenseMapper::toDto);
    }

    /**
     * Find all expenses for a holiday
     */
    public List<HolidayExpenseDTO> findByHolidayIdAsDto(final Long holidayId) {
        validateIdNotNull(holidayId);
        Holiday holiday = getHolidayById(holidayId);

        return holidayExpenseRepository.findByHolidayOrderByExpenseDateDesc(holiday).stream()
                .map(holidayExpenseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find expenses by holiday and type
     */
    public List<HolidayExpenseDTO> findByHolidayIdAndTypeAsDto(
            final Long holidayId,
            final HolidayExpenseType type) {
        validateIdNotNull(holidayId);
        Objects.requireNonNull(type, "Expense type must not be null");
        Holiday holiday = getHolidayById(holidayId);

        return holidayExpenseRepository.findByHolidayAndExpenseTypeOrderByExpenseDateDesc(holiday, type).stream()
                .map(holidayExpenseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete holiday expense by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting holiday expense with ID {}", id);

        if (!holidayExpenseRepository.existsById(id)) {
            throw new IllegalArgumentException("Holiday expense with ID " + id + " does not exist");
        }

        holidayExpenseRepository.deleteById(id);
    }

    /**
     * Calculate total spent for a holiday
     */
    public BigDecimal calculateTotalSpent(final Long holidayId) {
        validateIdNotNull(holidayId);
        Holiday holiday = getHolidayById(holidayId);

        return holidayExpenseRepository.calculateTotalSpent(holiday);
    }

    /**
     * Calculate spending by expense type for a holiday
     */
    public Map<HolidayExpenseType, BigDecimal> calculateSpendingByType(final Long holidayId) {
        validateIdNotNull(holidayId);
        Holiday holiday = getHolidayById(holidayId);

        List<HolidayExpense> expenses = holidayExpenseRepository.findByHolidayOrderByExpenseDateDesc(holiday);

        return expenses.stream()
                .collect(Collectors.groupingBy(
                        HolidayExpense::getExpenseType,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                HolidayExpense::getAmount,
                                BigDecimal::add
                        )
                ));
    }

    /**
     * Count expenses for a holiday
     */
    public long countExpenses(final Long holidayId) {
        validateIdNotNull(holidayId);
        Holiday holiday = getHolidayById(holidayId);
        return holidayExpenseRepository.countByHoliday(holiday);
    }

    /**
     * Calculate total transportation costs for a holiday
     */
    public BigDecimal calculateTransportationCosts(final Long holidayId) {
        validateIdNotNull(holidayId);
        Holiday holiday = getHolidayById(holidayId);

        List<HolidayExpense> expenses = holidayExpenseRepository.findByHolidayOrderByExpenseDateDesc(holiday);

        return expenses.stream()
                .filter(expense -> expense.getExpenseType().isTransportation())
                .map(HolidayExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total food costs for a holiday
     */
    public BigDecimal calculateFoodCosts(final Long holidayId) {
        validateIdNotNull(holidayId);
        Holiday holiday = getHolidayById(holidayId);

        List<HolidayExpense> expenses = holidayExpenseRepository.findByHolidayOrderByExpenseDateDesc(holiday);

        return expenses.stream()
                .filter(expense -> expense.getExpenseType().isFood())
                .map(HolidayExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Private helper methods

    private Holiday getHolidayById(final Long holidayId) {
        return holidayRepository.findById(holidayId)
                .orElseThrow(() -> new IllegalArgumentException("Holiday with ID " + holidayId + " not found"));
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    private void validateDtoNotNull(final HolidayExpenseDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("HolidayExpenseDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final HolidayExpenseDTO dto) {
        if (dto.getHolidayId() == null) {
            throw new IllegalArgumentException("Holiday ID must not be null");
        }
        if (dto.getExpenseType() == null) {
            throw new IllegalArgumentException("Expense type must not be null");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description must not be null or empty");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (dto.getExpenseDate() == null) {
            throw new IllegalArgumentException("Expense date must not be null");
        }
    }

    private void validateAmount(final HolidayExpenseDTO dto) {
        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
