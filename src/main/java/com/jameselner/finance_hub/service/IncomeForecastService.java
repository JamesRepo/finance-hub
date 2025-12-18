package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
import com.jameselner.finance_hub.dto.IncomeForecastDTO;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeForecastService {

    private final IncomeSourceRepository incomeSourceRepository;

    public IncomeForecastDTO generateForecast(final User user, final LocalDate startDate, final LocalDate endDate) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        List<IncomeSource> activeSources = incomeSourceRepository.findByUserAndIsActiveOrderByStartDateDesc(user, true);
        List<IncomeForecastDTO.ForecastedIncomeEntry> entries = new ArrayList<>();
        BigDecimal totalForecastedIncome = BigDecimal.ZERO;

        for (IncomeSource source : activeSources) {
            if (source.getIsRecurring()) {
                List<IncomeForecastDTO.ForecastedIncomeEntry> recurringEntries =
                        generateRecurringEntries(source, startDate, endDate);
                entries.addAll(recurringEntries);

                for (IncomeForecastDTO.ForecastedIncomeEntry entry : recurringEntries) {
                    totalForecastedIncome = totalForecastedIncome.add(entry.getAmount());
                }
            } else {
                if (isWithinPeriod(source.getStartDate(), startDate, endDate)) {
                    String categoryName = source.getCategory() != null ? source.getCategory().getCategoryName() : "Income";
                    IncomeForecastDTO.ForecastedIncomeEntry entry = IncomeForecastDTO.ForecastedIncomeEntry.builder()
                            .expectedDate(source.getStartDate())
                            .sourceName(categoryName)
                            .amount(source.getGrossAmount())
                            .frequency("One-time")
                            .build();
                    entries.add(entry);
                    totalForecastedIncome = totalForecastedIncome.add(source.getGrossAmount());
                }
            }
        }

        entries.sort((e1, e2) -> e1.getExpectedDate().compareTo(e2.getExpectedDate()));

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal averageWeeklyIncome = BigDecimal.ZERO;
        BigDecimal averageMonthlyIncome = BigDecimal.ZERO;

        if (daysBetween > 0) {
            BigDecimal weeksInPeriod = BigDecimal.valueOf(daysBetween).divide(
                    BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
            if (weeksInPeriod.compareTo(BigDecimal.ZERO) > 0) {
                averageWeeklyIncome = totalForecastedIncome.divide(weeksInPeriod, 2, RoundingMode.HALF_UP);
            }

            BigDecimal monthsInPeriod = BigDecimal.valueOf(daysBetween).divide(
                    BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            if (monthsInPeriod.compareTo(BigDecimal.ZERO) > 0) {
                averageMonthlyIncome = totalForecastedIncome.divide(monthsInPeriod, 2, RoundingMode.HALF_UP);
            }
        }

        return IncomeForecastDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalForecastedIncome(totalForecastedIncome)
                .averageMonthlyIncome(averageMonthlyIncome)
                .averageWeeklyIncome(averageWeeklyIncome)
                .entries(entries)
                .build();
    }

    public IncomeForecastDTO generateMonthlyForecast(final User user) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1);
        return generateForecast(user, startDate, endDate);
    }

    public IncomeForecastDTO generateQuarterlyForecast(final User user) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(3);
        return generateForecast(user, startDate, endDate);
    }

    public IncomeForecastDTO generateYearlyForecast(final User user) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);
        return generateForecast(user, startDate, endDate);
    }

    private List<IncomeForecastDTO.ForecastedIncomeEntry> generateRecurringEntries(
            final IncomeSource source,
            final LocalDate startDate,
            final LocalDate endDate) {

        List<IncomeForecastDTO.ForecastedIncomeEntry> entries = new ArrayList<>();
        LocalDate currentDate = source.getNextExpectedDate() != null ?
                source.getNextExpectedDate() : source.getStartDate();
        String categoryName = source.getCategory() != null ? source.getCategory().getCategoryName() : "Income";

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            if (currentDate.isAfter(startDate) || currentDate.isEqual(startDate)) {
                if (source.getEndDate() == null || currentDate.isBefore(source.getEndDate()) ||
                        currentDate.isEqual(source.getEndDate())) {

                    IncomeForecastDTO.ForecastedIncomeEntry entry = IncomeForecastDTO.ForecastedIncomeEntry.builder()
                            .expectedDate(currentDate)
                            .sourceName(categoryName)
                            .amount(source.getGrossAmount())
                            .frequency(source.getRecurrenceFrequency().getDisplayName())
                            .build();
                    entries.add(entry);
                }
            }

            currentDate = calculateNextOccurrence(currentDate, source.getRecurrenceFrequency());

            if (currentDate == null) {
                break;
            }
        }

        return entries;
    }

    private LocalDate calculateNextOccurrence(final LocalDate currentDate, final RecurrenceFrequency frequency) {
        if (currentDate == null || frequency == null) {
            return null;
        }

        return switch (frequency) {
            case ONE_TIME -> null;
            case WEEKLY -> currentDate.plusWeeks(1);
            case BI_WEEKLY -> currentDate.plusWeeks(2);
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentDate.plusMonths(6);
            case ANNUALLY -> currentDate.plusYears(1);
        };
    }

    private boolean isWithinPeriod(final LocalDate date, final LocalDate startDate, final LocalDate endDate) {
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
                (date.isEqual(endDate) || date.isBefore(endDate));
    }
}
