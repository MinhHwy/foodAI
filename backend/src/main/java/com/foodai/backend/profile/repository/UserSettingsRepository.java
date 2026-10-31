package com.foodai.backend.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodai.backend.profile.entity.UserSettingsEntity;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Long> {
}
