package com.jameselner.finance_hub.repository;

import com.jameselner.finance_hub.domain.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserUserId(Long userId);
}
