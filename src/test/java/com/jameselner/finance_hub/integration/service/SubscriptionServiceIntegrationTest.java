package com.jameselner.finance_hub.integration.service;

import com.jameselner.finance_hub.domain.Subscription;
import com.jameselner.finance_hub.domain.enums.SubscriptionFrequency;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory.TestUserContext;
import com.jameselner.finance_hub.repository.SubscriptionRepository;
import com.jameselner.finance_hub.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubscriptionService Integration Tests")
class SubscriptionServiceIntegrationTest extends TransactionalIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TestFixtureFactory fixtureFactory;

    private TestUserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = fixtureFactory.createBasicUser("subs-int-test@example.com");
    }

    @Test
    @DisplayName("Monthly total includes yearly subscription paid in that month")
    void monthlyTotalIncludesYearlySubscriptionPayment() {
        LocalDate now = LocalDate.now();

        Subscription monthly = new Subscription();
        monthly.setUser(userContext.user());
        monthly.setName("Streaming Monthly");
        monthly.setAmount(new BigDecimal("12.50"));
        monthly.setFrequency(SubscriptionFrequency.MONTHLY);
        monthly.setPaymentDate(now.withDayOfMonth(5));

        Subscription yearly = new Subscription();
        yearly.setUser(userContext.user());
        yearly.setName("Gym Annual");
        yearly.setAmount(new BigDecimal("240.00"));
        yearly.setFrequency(SubscriptionFrequency.YEARLY);
        yearly.setPaymentDate(now.withDayOfMonth(10));

        subscriptionRepository.save(monthly);
        subscriptionRepository.save(yearly);

        BigDecimal total = subscriptionService.calculateTotalForMonth(
                userContext.user().getUserId(), now.getYear(), now.getMonthValue());

        assertThat(total).isEqualByComparingTo(new BigDecimal("252.50"));
    }

    @Test
    @DisplayName("Count includes subscriptions on month boundaries")
    void countIncludesMonthBoundaries() {
        LocalDate now = LocalDate.now();
        LocalDate first = now.withDayOfMonth(1);
        LocalDate last = now.withDayOfMonth(now.lengthOfMonth());

        Subscription firstDay = new Subscription();
        firstDay.setUser(userContext.user());
        firstDay.setName("First Day");
        firstDay.setAmount(new BigDecimal("5.00"));
        firstDay.setFrequency(SubscriptionFrequency.MONTHLY);
        firstDay.setPaymentDate(first);

        Subscription lastDay = new Subscription();
        lastDay.setUser(userContext.user());
        lastDay.setName("Last Day");
        lastDay.setAmount(new BigDecimal("7.00"));
        lastDay.setFrequency(SubscriptionFrequency.MONTHLY);
        lastDay.setPaymentDate(last);

        Subscription outside = new Subscription();
        outside.setUser(userContext.user());
        outside.setName("Outside");
        outside.setAmount(new BigDecimal("9.00"));
        outside.setFrequency(SubscriptionFrequency.MONTHLY);
        outside.setPaymentDate(now.minusMonths(1).withDayOfMonth(15));

        subscriptionRepository.save(firstDay);
        subscriptionRepository.save(lastDay);
        subscriptionRepository.save(outside);

        long count = subscriptionService.countSubscriptionsForMonth(
                userContext.user().getUserId(), now.getYear(), now.getMonthValue());

        assertThat(count).isEqualTo(2L);
    }
}
