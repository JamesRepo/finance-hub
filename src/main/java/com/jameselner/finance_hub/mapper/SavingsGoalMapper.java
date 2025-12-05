package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.SavingsGoal;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.SavingsGoalDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavingsGoalMapper {

    private final UserRepository userRepository;

    public SavingsGoalDTO toDto(final SavingsGoal savingsGoal) {
        if (savingsGoal == null) {
            return null;
        }

        return SavingsGoalDTO.builder()
                .goalId(savingsGoal.getGoalId())
                .userId(savingsGoal.getUser() != null ? savingsGoal.getUser().getUserId() : null)
                .userName(savingsGoal.getUser() != null ? savingsGoal.getUser().getEmail() : null)
                .goalName(savingsGoal.getGoalName())
                .targetAmount(savingsGoal.getTargetAmount())
                .currentAmount(savingsGoal.getCurrentAmount())
                .targetDate(savingsGoal.getTargetDate())
                .priority(savingsGoal.getPriority())
                .createdAt(savingsGoal.getCreatedAt())
                .updatedAt(savingsGoal.getUpdatedAt())
                .build();
    }

    public SavingsGoal toEntity(final SavingsGoalDTO dto) {
        if (dto == null) {
            return null;
        }

        SavingsGoal savingsGoal = new SavingsGoal();
        updateEntityFromDto(savingsGoal, dto);
        return savingsGoal;
    }

    public void updateEntityFromDto(final SavingsGoal savingsGoal, final SavingsGoalDTO dto) {
        if (savingsGoal == null || dto == null) {
            throw new IllegalArgumentException("SavingsGoal and DTO must not be null");
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + dto.getUserId()));
            savingsGoal.setUser(user);
        }

        savingsGoal.setGoalName(dto.getGoalName());
        savingsGoal.setTargetAmount(dto.getTargetAmount());
        savingsGoal.setCurrentAmount(dto.getCurrentAmount() != null ? dto.getCurrentAmount() : savingsGoal.getCurrentAmount());
        savingsGoal.setTargetDate(dto.getTargetDate());
        savingsGoal.setPriority(dto.getPriority());
    }
}
