package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.AccountDTO;
import com.jameselner.finance_hub.mapper.AccountMapper;
import com.jameselner.finance_hub.repository.AccountRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    /**
     * Create a new account from DTO
     */
    @Transactional
    public AccountDTO createFromDto(final AccountDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getAccountId() != null) {
            throw new IllegalArgumentException("Account ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);

        Account account = accountMapper.toEntity(dto);

        // Set defaults if not provided
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        if (account.getCurrency() == null) {
            account.setCurrency("GBP");
        }
        if (account.getIsActive() == null) {
            account.setIsActive(true);
        }
        if (account.getIsDefault() == null) {
            account.setIsDefault(false);
        }

        // If this account is being set as default, clear default from other accounts
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            accountRepository.clearDefaultForUser(account.getUser());
        }

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDto(savedAccount);
    }

    /**
     * Update an existing account from DTO
     */
    @Transactional
    public AccountDTO updateFromDto(final AccountDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID must not be null for update operation");
        }

        Account existingAccount = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account with ID " + dto.getAccountId() + " does not exist"));

        validateDtoRequiredFields(dto);

        // If this account is being set as default, clear default from other accounts
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(existingAccount.getIsDefault())) {
            accountRepository.clearDefaultForUser(existingAccount.getUser());
        }

        accountMapper.updateEntityFromDto(existingAccount, dto);
        Account savedAccount = accountRepository.save(existingAccount);

        return accountMapper.toDto(savedAccount);
    }

    /**
     * Find the default account for a user
     */
    public Optional<Account> findDefaultByUserId(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return accountRepository.findByUserAndIsDefaultTrue(user);
    }

    /**
     * Find account by ID as DTO
     */
    public Optional<AccountDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return accountRepository.findById(id)
                .map(accountMapper::toDto);
    }

    /**
     * Find all accounts for a user
     */
    public List<AccountDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return accountRepository.findByUser(user).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find active accounts for a user
     */
    public List<AccountDTO> findActiveByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return accountRepository.findByUserAndIsActiveTrue(user).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete account by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        if (!accountRepository.existsById(id)) {
            throw new IllegalArgumentException("Account with ID " + id + " does not exist");
        }

        accountRepository.deleteById(id);
    }

    /**
     * Count accounts for a user
     */
    public long countByUserId(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return accountRepository.countByUser(user);
    }

    // Legacy methods - still used by TransactionForm
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
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

    private void validateDtoNotNull(final AccountDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AccountDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final AccountDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getAccountName() == null || dto.getAccountName().trim().isEmpty()) {
            throw new IllegalArgumentException("Account name must not be null or empty");
        }
        if (dto.getAccountType() == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
    }
}