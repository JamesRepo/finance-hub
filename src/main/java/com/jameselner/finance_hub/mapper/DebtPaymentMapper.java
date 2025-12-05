package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.DebtPayment;
import com.jameselner.finance_hub.dto.DebtPaymentDTO;
import com.jameselner.finance_hub.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebtPaymentMapper {

    private final DebtRepository debtRepository;

    public DebtPaymentDTO toDto(final DebtPayment debtPayment) {
        if (debtPayment == null) {
            return null;
        }

        return DebtPaymentDTO.builder()
                .paymentId(debtPayment.getPaymentId())
                .debtId(debtPayment.getDebt() != null ? debtPayment.getDebt().getDebtId() : null)
                .debtName(debtPayment.getDebt() != null ? debtPayment.getDebt().getDebtName() : null)
                .paymentAmount(debtPayment.getPaymentAmount())
                .principalPaid(debtPayment.getPrincipalPaid())
                .interestPaid(debtPayment.getInterestPaid())
                .paymentDate(debtPayment.getPaymentDate())
                .createdAt(debtPayment.getCreatedAt())
                .updatedAt(debtPayment.getUpdatedAt())
                .build();
    }

    public DebtPayment toEntity(final DebtPaymentDTO dto) {
        if (dto == null) {
            return null;
        }

        DebtPayment debtPayment = new DebtPayment();
        updateEntityFromDto(debtPayment, dto);
        return debtPayment;
    }

    public void updateEntityFromDto(final DebtPayment debtPayment, final DebtPaymentDTO dto) {
        if (debtPayment == null || dto == null) {
            throw new IllegalArgumentException("DebtPayment and DTO must not be null");
        }

        if (dto.getDebtId() != null) {
            Debt debt = debtRepository.findById(dto.getDebtId())
                    .orElseThrow(() -> new IllegalArgumentException("Debt not found with ID: " + dto.getDebtId()));
            debtPayment.setDebt(debt);
        }

        debtPayment.setPaymentAmount(dto.getPaymentAmount());
        debtPayment.setPrincipalPaid(dto.getPrincipalPaid());
        debtPayment.setInterestPaid(dto.getInterestPaid());
        debtPayment.setPaymentDate(dto.getPaymentDate());
    }
}
