package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.mapper.TransactionMapper;
import com.jameselner.finance_hub.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Create a new transaction from DTO
     */
    @Transactional
    public TransactionDTO createTransactionFromDto(final TransactionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getTransactionId() != null) {
            throw new IllegalArgumentException("Transaction ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto.getAmount());

        Transaction transaction = transactionMapper.toEntity(dto);
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    /**
     * Update an existing transaction from DTO
     */
    @Transactional
    public TransactionDTO updateTransactionFromDto(final TransactionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getTransactionId() == null) {
            throw new IllegalArgumentException("Transaction ID must not be null for update operation");
        }

        Transaction existingTransaction = transactionRepository.findById(dto.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction with ID " + dto.getTransactionId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto.getAmount());

        transactionMapper.updateEntityFromDto(existingTransaction, dto);
        Transaction savedTransaction = transactionRepository.save(existingTransaction);
        return transactionMapper.toDto(savedTransaction);
    }

    /**
     * Find a transaction by ID and return as DTO
     */
    public Optional<TransactionDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto);
    }

    /**
     * Find all transactions and return as DTOs
     */
    public List<TransactionDTO> findAllAsDto() {
        return transactionRepository.findAll().stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find transactions by year and month and return as DTOs
     */
    public List<TransactionDTO> findByYearAndMonthAsDto(final int year, final int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate)
                .stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find transactions by year, month and category type and return as DTOs
     */
    public List<TransactionDTO> findByYearAndMonthAndCategoryAsDto(
            final int year,
            final int month,
            final Category category) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return transactionRepository.findByTransactionDateBetweenAndCategoryOrderByTransactionDateDesc(
                startDate, endDate, category)
                .stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    /**
     * Delete a transaction by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting transaction with ID {}", id);

        if (!transactionRepository.existsById(id)) {
            throw new IllegalArgumentException("Transaction with ID " + id + " does not exist");
        }

        transactionRepository.deleteById(id);
    }

    /**
     * Find all distinct place/venue values for autocomplete
     */
    public List<String> findDistinctPlaceVenues() {
        return transactionRepository.findDistinctPlaceVenues();
    }

    /**
     * Search transactions by place/venue or description across all transactions
     */
    public List<TransactionDTO> searchByTermAsDto(final String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        return transactionRepository.searchByPlaceVenueOrDescription(searchTerm.trim())
                .stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get sum of transactions by user, type and date range
     */
    public BigDecimal sumByUserAndTypeAndDateRange(
            final User user,
            final TransactionType type,
            final LocalDate startDate,
            final LocalDate endDate
    ) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type must not be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        return transactionRepository.sumByUserAndTypeAndDateRange(user, type, startDate, endDate);
    }

    // Private validation methods

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID must not be null");
        }
    }

    private void validateAmount(final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateDtoNotNull(final TransactionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TransactionDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final TransactionDTO dto) {
        if (dto.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID must not be null");
        }
        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID must not be null");
        }
        if (dto.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type must not be null");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (dto.getTransactionDate() == null) {
            throw new IllegalArgumentException("Transaction date must not be null");
        }
    }
}
