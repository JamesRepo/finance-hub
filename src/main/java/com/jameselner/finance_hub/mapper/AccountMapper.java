package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.AccountDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountMapper {

    private final UserRepository userRepository;

    public AccountDTO toDto(final Account account) {
        if (account == null) {
            return null;
        }

        return AccountDTO.builder()
                .accountId(account.getAccountId())
                .userId(account.getUser() != null ? account.getUser().getUserId() : null)
                .userEmail(account.getUser() != null ? account.getUser().getEmail() : null)
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .isActive(account.getIsActive())
                .isDefault(account.getIsDefault())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    public Account toEntity(final AccountDTO dto) {
        if (dto == null) {
            return null;
        }

        Account account = new Account();
        updateEntityFromDto(account, dto);
        return account;
    }

    public void updateEntityFromDto(final Account account, final AccountDTO dto) {
        if (account == null || dto == null) {
            return;
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User with ID " + dto.getUserId() + " not found"));
            account.setUser(user);
        }

        account.setAccountName(dto.getAccountName());
        account.setAccountType(dto.getAccountType());

        if (dto.getBalance() != null) {
            account.setBalance(dto.getBalance());
        }

        if (dto.getCurrency() != null) {
            account.setCurrency(dto.getCurrency());
        }

        if (dto.getIsActive() != null) {
            account.setIsActive(dto.getIsActive());
        }

        if (dto.getIsDefault() != null) {
            account.setIsDefault(dto.getIsDefault());
        }
    }
}
