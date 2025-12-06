package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.HolidayDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HolidayMapper {

    private final UserRepository userRepository;

    public HolidayDTO toDto(final Holiday holiday) {
        if (holiday == null) {
            return null;
        }

        return HolidayDTO.builder()
                .holidayId(holiday.getHolidayId())
                .userId(holiday.getUser() != null ? holiday.getUser().getUserId() : null)
                .userEmail(holiday.getUser() != null ? holiday.getUser().getEmail() : null)
                .name(holiday.getName())
                .destination(holiday.getDestination())
                .startDate(holiday.getStartDate())
                .endDate(holiday.getEndDate())
                .budget(holiday.getBudget())
                .description(holiday.getDescription())
                .isActive(holiday.getIsActive())
                .createdAt(holiday.getCreatedAt())
                .updatedAt(holiday.getUpdatedAt())
                .build();
    }

    public Holiday toEntity(final HolidayDTO dto) {
        if (dto == null) {
            return null;
        }

        Holiday holiday = new Holiday();
        updateEntityFromDto(holiday, dto);
        return holiday;
    }

    public void updateEntityFromDto(final Holiday holiday, final HolidayDTO dto) {
        if (holiday == null || dto == null) {
            return;
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User with ID " + dto.getUserId() + " not found"));
            holiday.setUser(user);
        }

        holiday.setName(dto.getName());
        holiday.setDestination(dto.getDestination());
        holiday.setStartDate(dto.getStartDate());
        holiday.setEndDate(dto.getEndDate());
        holiday.setBudget(dto.getBudget());
        holiday.setDescription(dto.getDescription());
        holiday.setIsActive(dto.getIsActive());
    }
}