package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.repository.AccountRepository;
import com.jameselner.finance_hub.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Convert Transaction entity to TransactionDTO
     */
    public TransactionDTO toDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccount() != null ? transaction.getAccount().getAccountId() : null)
                .accountName(transaction.getAccount() != null ? transaction.getAccount().getAccountName() : null)
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getCategoryId() : null)
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getCategoryName() : null)
                .categoryColorCode(transaction.getCategory() != null ? transaction.getCategory().getColorCode() : null)
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .notes(transaction.getNotes())
                .paymentMethod(transaction.getPaymentMethod())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    /**
     * Convert TransactionDTO to Transaction entity (for create operations)
     * This method fetches the Account and Category entities from the database
     */
    public Transaction toEntity(TransactionDTO dto) {
        if (dto == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        updateEntityFromDto(transaction, dto);
        return transaction;
    }

    /**
     * Update an existing Transaction entity from a TransactionDTO
     * This method fetches the Account and Category entities from the database
     */
    public void updateEntityFromDto(Transaction transaction, TransactionDTO dto) {
        if (transaction == null || dto == null) {
            throw new IllegalArgumentException("Transaction and DTO must not be null");
        }

        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + dto.getAccountId()));
            transaction.setAccount(account);
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dto.getCategoryId()));
            transaction.setCategory(category);
        }

        transaction.setTransactionType(dto.getTransactionType());
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setDescription(dto.getDescription());
        transaction.setNotes(dto.getNotes());
        transaction.setPaymentMethod(dto.getPaymentMethod());
    }
}