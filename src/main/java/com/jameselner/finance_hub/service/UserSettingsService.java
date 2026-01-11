package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.UserSettings;
import com.jameselner.finance_hub.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    @Transactional
    public UserSettings getSettingsForUser(final User user) {
        return userSettingsRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> createDefaultSettings(user));
    }

    @Transactional
    public UserSettings createDefaultSettings(final User user) {
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setCurrency("GBP");
        settings.setLocale("en-GB");
        return userSettingsRepository.save(settings);
    }

    @Transactional
    public void updateCurrency(final User user, final String currency) {
        UserSettings settings = getSettingsForUser(user);
        settings.setCurrency(currency);
        userSettingsRepository.save(settings);
    }
}
