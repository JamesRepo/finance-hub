package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Subscription;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.SubscriptionFrequency;
import com.jameselner.finance_hub.dto.SubscriptionDTO;
import com.jameselner.finance_hub.mapper.SubscriptionMapper;
import com.jameselner.finance_hub.repository.SubscriptionRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * Create a new subscription from DTO
     */
    @Transactional
    public SubscriptionDTO createFromDto(final SubscriptionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getSubscriptionId() != null) {
            throw new IllegalArgumentException("Subscription ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto);

        Subscription subscription = subscriptionMapper.toEntity(dto);
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return enrichWithCalculatedFields(subscriptionMapper.toDto(savedSubscription));
    }

    /**
     * Update an existing subscription from DTO
     */
    @Transactional
    public SubscriptionDTO updateFromDto(final SubscriptionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getSubscriptionId() == null) {
            throw new IllegalArgumentException("Subscription ID must not be null for update operation");
        }

        Subscription existingSubscription = subscriptionRepository.findById(dto.getSubscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription with ID " + dto.getSubscriptionId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto);

        subscriptionMapper.updateEntityFromDto(existingSubscription, dto);
        Subscription savedSubscription = subscriptionRepository.save(existingSubscription);

        return enrichWithCalculatedFields(subscriptionMapper.toDto(savedSubscription));
    }

    /**
     * Find subscription by ID as DTO
     */
    public Optional<SubscriptionDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return subscriptionRepository.findById(id)
                .map(subscriptionMapper::toDto)
                .map(this::enrichWithCalculatedFields);
    }

    /**
     * Find all subscriptions for a user
     */
    public List<SubscriptionDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return subscriptionRepository.findByUserOrderByPaymentDateDesc(user).stream()
                .map(subscriptionMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find subscriptions for current month
     */
    public List<SubscriptionDTO> findCurrentMonthByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        return subscriptionRepository.findByUserAndPaymentDateBetween(user, startOfMonth, endOfMonth).stream()
                .map(subscriptionMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find subscriptions for a specific month
     */
    public List<SubscriptionDTO> findByUserIdAndMonthAsDto(final Long userId, final int year, final int month) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        return subscriptionRepository.findByUserAndPaymentDateBetween(user, startOfMonth, endOfMonth).stream()
                .map(subscriptionMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Calculate total for a specific month
     */
    public BigDecimal calculateTotalForMonth(final Long userId, final int year, final int month) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        return subscriptionRepository.calculateTotalForPeriod(user, startOfMonth, endOfMonth);
    }

    /**
     * Count subscriptions for a specific month
     */
    public long countSubscriptionsForMonth(final Long userId, final int year, final int month) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        return subscriptionRepository.countByUserAndPaymentDateBetween(user, startOfMonth, endOfMonth);
    }

    /**
     * Find subscriptions by frequency
     */
    public List<SubscriptionDTO> findByUserIdAndFrequencyAsDto(
            final Long userId,
            final SubscriptionFrequency frequency) {
        validateIdNotNull(userId);
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency must not be null");
        }
        User user = getUserById(userId);

        return subscriptionRepository.findByUserAndFrequencyOrderByPaymentDateDesc(user, frequency).stream()
                .map(subscriptionMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Delete subscription by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting subscription with ID {}", id);

        if (!subscriptionRepository.existsById(id)) {
            throw new IllegalArgumentException("Subscription with ID " + id + " does not exist");
        }

        subscriptionRepository.deleteById(id);
    }

    /**
     * Copy subscriptions from a previous month to current month
     * Creates new subscription entries with payment date set to current month
     */
    @Transactional
    public List<SubscriptionDTO> copyFromPreviousMonth(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        LocalDate now = LocalDate.now();
        LocalDate lastMonth = now.minusMonths(1);
        LocalDate currentMonthStart = now.withDayOfMonth(1);
        LocalDate currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());

        log.debug("Copying subscriptions from {}/{} to {}/{} for user {}",
                lastMonth.getYear(), lastMonth.getMonthValue(),
                now.getYear(), now.getMonthValue(), userId);

        // Get subscriptions from last month
        LocalDate lastMonthStart = lastMonth.withDayOfMonth(1);
        LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());
        List<Subscription> lastMonthSubs = subscriptionRepository.findByUserAndPaymentDateBetween(
                user, lastMonthStart, lastMonthEnd);

        if (lastMonthSubs.isEmpty()) {
            return List.of();
        }

        List<Subscription> currentMonthSubs = subscriptionRepository.findByUserAndPaymentDateBetween(
                user, currentMonthStart, currentMonthEnd);
        java.util.Set<String> existingKeys = currentMonthSubs.stream()
                .map(sub -> buildCopyKey(sub.getName(), sub.getFrequency(), sub.getAmount(), sub.getDescription()))
                .collect(Collectors.toSet());

        // Create new subscriptions for current month
        List<Subscription> newSubscriptions = lastMonthSubs.stream()
                .filter(sub -> sub.getFrequency() == SubscriptionFrequency.MONTHLY)
                .filter(sub -> !existingKeys.contains(buildCopyKey(
                        sub.getName(), sub.getFrequency(), sub.getAmount(), sub.getDescription())))
                .map(source -> {
                    Subscription newSub = new Subscription();
                    newSub.setUser(user);
                    newSub.setName(source.getName());
                    newSub.setAmount(source.getAmount());
                    newSub.setFrequency(source.getFrequency());
                    int day = Math.min(source.getPaymentDate().getDayOfMonth(), now.lengthOfMonth());
                    newSub.setPaymentDate(LocalDate.of(now.getYear(), now.getMonth(), day));
                    newSub.setDescription(source.getDescription());
                    return newSub;
                })
                .collect(Collectors.toList());

        if (newSubscriptions.isEmpty()) {
            return List.of();
        }

        List<Subscription> saved = subscriptionRepository.saveAll(newSubscriptions);

        log.debug("Copied {} subscriptions", saved.size());

        return saved.stream()
                .map(subscriptionMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Calculate monthly equivalent total for a specific month
     */
    public BigDecimal calculateMonthlyEquivalentTotalForMonth(final Long userId, final int year, final int month) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        return subscriptionRepository.findByUserAndPaymentDateBetween(user, startOfMonth, endOfMonth).stream()
                .filter(sub -> sub.getAmount() != null && sub.getFrequency() != null)
                .map(sub -> sub.getFrequency().toMonthlyEquivalent(sub.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total for current month
     */
    public BigDecimal calculateCurrentMonthTotal(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        return subscriptionRepository.calculateTotalForPeriod(user, startOfMonth, endOfMonth);
    }

    /**
     * Calculate total for a date range
     */
    public BigDecimal calculateTotalForPeriod(final Long userId, final LocalDate startDate, final LocalDate endDate) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return subscriptionRepository.calculateTotalForPeriod(user, startDate, endDate);
    }

    /**
     * Count subscriptions for a user
     */
    public long countSubscriptions(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return subscriptionRepository.countByUser(user);
    }

    /**
     * Count subscriptions for current month
     */
    public long countCurrentMonthSubscriptions(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        return subscriptionRepository.countByUserAndPaymentDateBetween(user, startOfMonth, endOfMonth);
    }

    // Private helper methods

    /**
     * Enrich DTO with calculated fields
     */
    private SubscriptionDTO enrichWithCalculatedFields(final SubscriptionDTO dto) {
        if (dto == null || dto.getAmount() == null || dto.getFrequency() == null) {
            return dto;
        }

        // Calculate monthly and annual equivalents
        dto.setMonthlyEquivalent(dto.getFrequency().toMonthlyEquivalent(dto.getAmount()));
        dto.setAnnualEquivalent(dto.getFrequency().toAnnualEquivalent(dto.getAmount()));

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

    private void validateDtoNotNull(final SubscriptionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("SubscriptionDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final SubscriptionDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Subscription name must not be null or empty");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (dto.getFrequency() == null) {
            throw new IllegalArgumentException("Frequency must not be null");
        }
        if (dto.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date must not be null");
        }
    }

    private void validateAmount(final SubscriptionDTO dto) {
        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private String buildCopyKey(final String name, final SubscriptionFrequency frequency, final BigDecimal amount, final String description) {
        return normalizeText(name) + "|"
                + frequency + "|"
                + normalizeAmount(amount) + "|"
                + normalizeText(description);
    }

    private String normalizeText(final String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeAmount(final BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }
}
