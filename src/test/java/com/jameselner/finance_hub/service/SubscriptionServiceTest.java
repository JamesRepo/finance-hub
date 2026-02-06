package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Subscription;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.SubscriptionFrequency;
import com.jameselner.finance_hub.dto.SubscriptionDTO;
import com.jameselner.finance_hub.mapper.SubscriptionMapper;
import com.jameselner.finance_hub.repository.SubscriptionRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Tests")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("calculateMonthlyEquivalentTotalForMonth")
    class CalculateMonthlyEquivalentTotalForMonthTests {

        @Test
        @DisplayName("sums monthly equivalents and skips nulls")
        void sumsMonthlyEquivalentsAndSkipsNulls() {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();

            Subscription monthly = new Subscription();
            monthly.setUser(testUser);
            monthly.setAmount(new BigDecimal("10.00"));
            monthly.setFrequency(SubscriptionFrequency.MONTHLY);

            Subscription yearly = new Subscription();
            yearly.setUser(testUser);
            yearly.setAmount(new BigDecimal("120.00"));
            yearly.setFrequency(SubscriptionFrequency.YEARLY);

            Subscription missingAmount = new Subscription();
            missingAmount.setUser(testUser);
            missingAmount.setAmount(null);
            missingAmount.setFrequency(SubscriptionFrequency.MONTHLY);

            Subscription missingFrequency = new Subscription();
            missingFrequency.setUser(testUser);
            missingFrequency.setAmount(new BigDecimal("5.00"));
            missingFrequency.setFrequency(null);

            when(subscriptionRepository.findByUserAndPaymentDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(monthly, yearly, missingAmount, missingFrequency));

            BigDecimal result = subscriptionService.calculateMonthlyEquivalentTotalForMonth(1L, year, month);

            BigDecimal expected = new BigDecimal("20.00");
            assertThat(result).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("copyFromPreviousMonth")
    class CopyFromPreviousMonthTests {

        @Test
        @DisplayName("copies only monthly subscriptions, skips duplicates, and preserves day-of-month")
        void copiesMonthlyOnlySkipsDuplicatesAndPreservesDay() {
            LocalDate now = LocalDate.now();
            LocalDate lastMonth = now.minusMonths(1);

            Subscription lastMonthMonthly = new Subscription();
            lastMonthMonthly.setUser(testUser);
            lastMonthMonthly.setName("Netflix");
            lastMonthMonthly.setAmount(new BigDecimal("12.99"));
            lastMonthMonthly.setFrequency(SubscriptionFrequency.MONTHLY);
            lastMonthMonthly.setPaymentDate(lastMonth.withDayOfMonth(15));
            lastMonthMonthly.setDescription("Streaming");

            Subscription lastMonthYearly = new Subscription();
            lastMonthYearly.setUser(testUser);
            lastMonthYearly.setName("Gym");
            lastMonthYearly.setAmount(new BigDecimal("240.00"));
            lastMonthYearly.setFrequency(SubscriptionFrequency.YEARLY);
            lastMonthYearly.setPaymentDate(lastMonth.withDayOfMonth(10));

            Subscription lastMonthMonthlyNew = new Subscription();
            lastMonthMonthlyNew.setUser(testUser);
            lastMonthMonthlyNew.setName("Spotify");
            lastMonthMonthlyNew.setAmount(new BigDecimal("9.99"));
            lastMonthMonthlyNew.setFrequency(SubscriptionFrequency.MONTHLY);
            lastMonthMonthlyNew.setPaymentDate(lastMonth.withDayOfMonth(Math.min(31, lastMonth.lengthOfMonth())));

            Subscription existingCurrent = new Subscription();
            existingCurrent.setUser(testUser);
            existingCurrent.setName("Netflix");
            existingCurrent.setAmount(new BigDecimal("12.99"));
            existingCurrent.setFrequency(SubscriptionFrequency.MONTHLY);
            existingCurrent.setPaymentDate(now.withDayOfMonth(1));
            existingCurrent.setDescription("Streaming");

            when(subscriptionRepository.findByUserAndPaymentDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                    .thenAnswer(invocation -> {
                        LocalDate start = invocation.getArgument(1);
                        if (start.getYear() == lastMonth.getYear() && start.getMonth() == lastMonth.getMonth()) {
                            return List.of(lastMonthMonthly, lastMonthYearly, lastMonthMonthlyNew);
                        }
                        if (start.getYear() == now.getYear() && start.getMonth() == now.getMonth()) {
                            return List.of(existingCurrent);
                        }
                        return List.of();
                    });

            when(subscriptionRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(subscriptionMapper.toDto(any(Subscription.class)))
                    .thenAnswer(invocation -> {
                        Subscription sub = invocation.getArgument(0);
                        return SubscriptionDTO.builder()
                                .subscriptionId(sub.getSubscriptionId())
                                .userId(testUser.getUserId())
                                .name(sub.getName())
                                .amount(sub.getAmount())
                                .frequency(sub.getFrequency())
                                .paymentDate(sub.getPaymentDate())
                                .description(sub.getDescription())
                                .build();
                    });

            List<SubscriptionDTO> result = subscriptionService.copyFromPreviousMonth(1L);

            ArgumentCaptor<List<Subscription>> captor = ArgumentCaptor.forClass(List.class);
            verify(subscriptionRepository).saveAll(captor.capture());

            List<Subscription> saved = captor.getValue();
            assertEquals(1, saved.size());
            Subscription copied = saved.get(0);
            assertEquals("Spotify", copied.getName());
            assertEquals(SubscriptionFrequency.MONTHLY, copied.getFrequency());

            int expectedDay = Math.min(lastMonthMonthlyNew.getPaymentDate().getDayOfMonth(), now.lengthOfMonth());
            assertEquals(expectedDay, copied.getPaymentDate().getDayOfMonth());
            assertEquals(now.getMonth(), copied.getPaymentDate().getMonth());

            assertEquals(1, result.size());
            assertEquals("Spotify", result.get(0).getName());
        }

        @Test
        @DisplayName("returns empty when no new subscriptions to copy")
        void returnsEmptyWhenNoNewSubscriptionsToCopy() {
            LocalDate now = LocalDate.now();
            LocalDate lastMonth = now.minusMonths(1);

            when(subscriptionRepository.findByUserAndPaymentDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                    .thenAnswer(invocation -> {
                        LocalDate start = invocation.getArgument(1);
                        if (start.getYear() == lastMonth.getYear() && start.getMonth() == lastMonth.getMonth()) {
                            return List.of();
                        }
                        return List.of();
                    });

            List<SubscriptionDTO> result = subscriptionService.copyFromPreviousMonth(1L);

            assertTrue(result.isEmpty());
            verify(subscriptionRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("date range queries")
    class DateRangeQueryTests {

        @Test
        @DisplayName("calculateTotalForMonth uses date range")
        void calculateTotalForMonthUsesDateRange() {
            LocalDate now = LocalDate.now();
            BigDecimal expected = new BigDecimal("25.00");

            when(subscriptionRepository.calculateTotalForPeriod(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(expected);

            BigDecimal result = subscriptionService.calculateTotalForMonth(1L, now.getYear(), now.getMonthValue());

            ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);

            verify(subscriptionRepository).calculateTotalForPeriod(eq(testUser), startCaptor.capture(), endCaptor.capture());
            assertEquals(now.withDayOfMonth(1), startCaptor.getValue());
            assertEquals(now.withDayOfMonth(now.lengthOfMonth()), endCaptor.getValue());
            assertThat(result).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("countSubscriptionsForMonth uses date range")
        void countSubscriptionsForMonthUsesDateRange() {
            LocalDate now = LocalDate.now();

            when(subscriptionRepository.countByUserAndPaymentDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(3L);

            long result = subscriptionService.countSubscriptionsForMonth(1L, now.getYear(), now.getMonthValue());

            ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);

            verify(subscriptionRepository).countByUserAndPaymentDateBetween(eq(testUser), startCaptor.capture(), endCaptor.capture());
            assertEquals(now.withDayOfMonth(1), startCaptor.getValue());
            assertEquals(now.withDayOfMonth(now.lengthOfMonth()), endCaptor.getValue());
            assertEquals(3L, result);
        }

        @Test
        @DisplayName("findByUserIdAndMonthAsDto enriches calculated fields")
        void findByUserIdAndMonthAsDtoEnrichesCalculatedFields() {
            LocalDate now = LocalDate.now();

            Subscription sub = new Subscription();
            sub.setUser(testUser);
            sub.setAmount(new BigDecimal("120.00"));
            sub.setFrequency(SubscriptionFrequency.YEARLY);
            sub.setPaymentDate(now);

            when(subscriptionRepository.findByUserAndPaymentDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(sub));

            SubscriptionDTO dto = SubscriptionDTO.builder()
                    .subscriptionId(1L)
                    .userId(testUser.getUserId())
                    .name("Annual Plan")
                    .amount(new BigDecimal("120.00"))
                    .frequency(SubscriptionFrequency.YEARLY)
                    .paymentDate(now)
                    .build();

            when(subscriptionMapper.toDto(sub)).thenReturn(dto);

            List<SubscriptionDTO> result = subscriptionService.findByUserIdAndMonthAsDto(1L, now.getYear(), now.getMonthValue());

            assertEquals(1, result.size());
            SubscriptionDTO enriched = result.get(0);
            assertThat(enriched.getMonthlyEquivalent()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(enriched.getAnnualEquivalent()).isEqualByComparingTo(new BigDecimal("120.00"));
        }
    }
}
