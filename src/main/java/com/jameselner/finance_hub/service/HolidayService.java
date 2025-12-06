package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.HolidayDTO;
import com.jameselner.finance_hub.mapper.HolidayMapper;
import com.jameselner.finance_hub.repository.HolidayExpenseRepository;
import com.jameselner.finance_hub.repository.HolidayRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayExpenseRepository holidayExpenseRepository;
    private final UserRepository userRepository;
    private final HolidayMapper holidayMapper;

    /**
     * Create a new holiday from DTO
     */
    @Transactional
    public HolidayDTO createFromDto(final HolidayDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getHolidayId() != null) {
            throw new IllegalArgumentException("Holiday ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateDates(dto);
        validateBudget(dto);

        if (dto.getIsActive() == null) {
            dto.setIsActive(true);
        }

        Holiday holiday = holidayMapper.toEntity(dto);
        Holiday savedHoliday = holidayRepository.save(holiday);

        return enrichWithCalculatedFields(holidayMapper.toDto(savedHoliday));
    }

    /**
     * Update an existing holiday from DTO
     */
    @Transactional
    public HolidayDTO updateFromDto(final HolidayDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getHolidayId() == null) {
            throw new IllegalArgumentException("Holiday ID must not be null for update operation");
        }

        Holiday existingHoliday = holidayRepository.findById(dto.getHolidayId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Holiday with ID " + dto.getHolidayId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateDates(dto);
        validateBudget(dto);

        holidayMapper.updateEntityFromDto(existingHoliday, dto);
        Holiday savedHoliday = holidayRepository.save(existingHoliday);

        return enrichWithCalculatedFields(holidayMapper.toDto(savedHoliday));
    }

    /**
     * Find holiday by ID as DTO
     */
    public Optional<HolidayDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return holidayRepository.findById(id)
                .map(holidayMapper::toDto)
                .map(this::enrichWithCalculatedFields);
    }

    /**
     * Find all holidays for a user
     */
    public List<HolidayDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return holidayRepository.findByUserOrderByStartDateDesc(user).stream()
                .map(holidayMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find active holidays for a user
     */
    public List<HolidayDTO> findActiveByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return holidayRepository.findByUserAndIsActiveTrueOrderByStartDateDesc(user).stream()
                .map(holidayMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find upcoming holidays for a user
     */
    public List<HolidayDTO> findUpcomingHolidaysAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return holidayRepository.findUpcomingHolidays(user, LocalDate.now()).stream()
                .map(holidayMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find past holidays for a user
     */
    public List<HolidayDTO> findPastHolidaysAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return holidayRepository.findPastHolidays(user, LocalDate.now()).stream()
                .map(holidayMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find current holidays for a user
     */
    public List<HolidayDTO> findCurrentHolidaysAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return holidayRepository.findCurrentHolidays(user, LocalDate.now()).stream()
                .map(holidayMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Delete holiday by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting holiday with ID {}", id);

        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException("Holiday with ID " + id + " does not exist");
        }

        holidayRepository.deleteById(id);
    }

    /**
     * Calculate total holiday spending for a user
     */
    public BigDecimal calculateTotalHolidaySpending(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        List<Holiday> holidays = holidayRepository.findByUserOrderByStartDateDesc(user);

        return holidays.stream()
                .map(holidayExpenseRepository::calculateTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total holiday budgets for a user
     */
    public BigDecimal calculateTotalHolidayBudgets(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        List<Holiday> activeHolidays = holidayRepository.findByUserAndIsActiveTrueOrderByStartDateDesc(user);

        return activeHolidays.stream()
                .map(Holiday::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Count active holidays for a user
     */
    public long countActiveHolidays(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return holidayRepository.countByUserAndIsActiveTrue(user);
    }

    // Private helper methods

    /**
     * Enrich DTO with calculated fields
     */
    private HolidayDTO enrichWithCalculatedFields(final HolidayDTO dto) {
        if (dto == null || dto.getHolidayId() == null) {
            return dto;
        }

        Holiday holiday = holidayRepository.findById(dto.getHolidayId()).orElse(null);
        if (holiday == null) {
            return dto;
        }

        // Calculate total spent
        BigDecimal totalSpent = holidayExpenseRepository.calculateTotalSpent(holiday);
        dto.setTotalSpent(totalSpent);

        // Calculate budget remaining
        if (dto.getBudget() != null) {
            dto.setBudgetRemaining(dto.getBudget().subtract(totalSpent));
        }

        // Calculate number of days
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
            dto.setNumberOfDays((int) days);

            // Calculate daily average spent
            if (days > 0 && totalSpent != null) {
                dto.setDailyAverage(totalSpent.divide(
                        BigDecimal.valueOf(days),
                        2,
                        RoundingMode.HALF_UP));
            }
        }

        // Count expenses
        long expenseCount = holidayExpenseRepository.countByHoliday(holiday);
        dto.setExpenseCount((int) expenseCount);

        return dto;
    }

    private User getUserById(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    private void validateDtoNotNull(final HolidayDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("HolidayDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final HolidayDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Holiday name must not be null or empty");
        }
        if (dto.getDestination() == null || dto.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination must not be null or empty");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (dto.getEndDate() == null) {
            throw new IllegalArgumentException("End date must not be null");
        }
        if (dto.getBudget() == null) {
            throw new IllegalArgumentException("Budget must not be null");
        }
    }

    private void validateDates(final HolidayDTO dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new IllegalArgumentException("End date must not be before start date");
            }
        }
    }

    private void validateBudget(final HolidayDTO dto) {
        if (dto.getBudget() != null && dto.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget must be greater than zero");
        }
    }
}