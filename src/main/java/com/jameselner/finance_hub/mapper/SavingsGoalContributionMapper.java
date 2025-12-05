package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.SavingsGoalContribution;
import com.jameselner.finance_hub.dto.SavingsGoalContributionDTO;
import com.jameselner.finance_hub.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavingsGoalContributionMapper {

    private final SavingsGoalRepository savingsGoalRepository;

    public SavingsGoalContributionDTO toDto(final SavingsGoalContribution contribution) {
        if (contribution == null) {
            return null;
        }

        return SavingsGoalContributionDTO.builder()
                .contributionId(contribution.getContributionId())
                .goalId(contribution.getSavingsGoal() != null ? contribution.getSavingsGoal().getGoalId() : null)
                .goalName(contribution.getSavingsGoal() != null ? contribution.getSavingsGoal().getGoalName() : null)
                .amount(contribution.getAmount())
                .contributionDate(contribution.getContributionDate())
                .note(contribution.getNote())
                .createdAt(contribution.getCreatedAt())
                .updatedAt(contribution.getUpdatedAt())
                .build();
    }

    public SavingsGoalContribution toEntity(final SavingsGoalContributionDTO dto) {
        if (dto == null) {
            return null;
        }

        SavingsGoalContribution contribution = new SavingsGoalContribution();
        updateEntityFromDto(contribution, dto);
        return contribution;
    }

    public void updateEntityFromDto(final SavingsGoalContribution contribution, final SavingsGoalContributionDTO dto) {
        if (contribution == null || dto == null) {
            throw new IllegalArgumentException("SavingsGoalContribution and DTO must not be null");
        }

        if (dto.getGoalId() != null) {
            SavingsGoal savingsGoal = savingsGoalRepository.findById(dto.getGoalId())
                    .orElseThrow(() -> new IllegalArgumentException("Savings goal not found with ID: " + dto.getGoalId()));
            contribution.setSavingsGoal(savingsGoal);
        }

        contribution.setAmount(dto.getAmount());
        contribution.setContributionDate(dto.getContributionDate());
        contribution.setNote(dto.getNote());
    }
}
