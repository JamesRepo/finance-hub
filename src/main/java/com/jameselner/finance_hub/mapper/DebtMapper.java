package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebtMapper {

    private final UserRepository userRepository;

    public DebtDTO toDto(final Debt debt) {
        if (debt == null) {
            return null;
        }

        return DebtDTO.builder()
                .debtId(debt.getDebtId())
                .userId(debt.getUser() != null ? debt.getUser().getUserId() : null)
                .userName(debt.getUser() != null ? debt.getUser().getEmail() : null)
                .debtName(debt.getDebtName())
                .debtType(debt.getDebtType())
                .principalAmount(debt.getPrincipalAmount())
                .currentBalance(debt.getCurrentBalance())
                .interestRate(debt.getInterestRate())
                .startDate(debt.getStartDate())
                .targetPayoffDate(debt.getTargetPayoffDate())
                .minimumPayment(debt.getMinimumPayment())
                .notes(debt.getNotes())
                .active(debt.getActive())
                .createdAt(debt.getCreatedAt())
                .updatedAt(debt.getUpdatedAt())
                .build();
    }

    public Debt toEntity(final DebtDTO dto) {
        if (dto == null) {
            return null;
        }

        Debt debt = new Debt();
        updateEntityFromDto(debt, dto);
        return debt;
    }

    public void updateEntityFromDto(final Debt debt, final DebtDTO dto) {
        if (debt == null || dto == null) {
            throw new IllegalArgumentException("Debt and DTO must not be null");
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + dto.getUserId()));
            debt.setUser(user);
        }

        debt.setDebtName(dto.getDebtName());
        debt.setDebtType(dto.getDebtType());
        debt.setPrincipalAmount(dto.getPrincipalAmount());
        debt.setCurrentBalance(dto.getCurrentBalance());
        debt.setInterestRate(dto.getInterestRate());
        debt.setStartDate(dto.getStartDate());
        debt.setTargetPayoffDate(dto.getTargetPayoffDate());
        debt.setMinimumPayment(dto.getMinimumPayment());
        debt.setNotes(dto.getNotes());
        if (dto.getActive() != null) {
            debt.setActive(dto.getActive());
        }
    }
}
