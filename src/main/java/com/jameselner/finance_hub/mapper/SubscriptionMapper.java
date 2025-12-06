package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Subscription;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.SubscriptionDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionMapper {

    private final UserRepository userRepository;

    public SubscriptionDTO toDto(final Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionDTO.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .userId(subscription.getUser() != null ? subscription.getUser().getUserId() : null)
                .userEmail(subscription.getUser() != null ? subscription.getUser().getEmail() : null)
                .name(subscription.getName())
                .amount(subscription.getAmount())
                .frequency(subscription.getFrequency())
                .paymentDate(subscription.getPaymentDate())
                .description(subscription.getDescription())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    public Subscription toEntity(final SubscriptionDTO dto) {
        if (dto == null) {
            return null;
        }

        Subscription subscription = new Subscription();
        updateEntityFromDto(subscription, dto);
        return subscription;
    }

    public void updateEntityFromDto(final Subscription subscription, final SubscriptionDTO dto) {
        if (subscription == null || dto == null) {
            return;
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User with ID " + dto.getUserId() + " not found"));
            subscription.setUser(user);
        }

        subscription.setName(dto.getName());
        subscription.setAmount(dto.getAmount());
        subscription.setFrequency(dto.getFrequency());
        subscription.setPaymentDate(dto.getPaymentDate());
        subscription.setDescription(dto.getDescription());
    }
}
