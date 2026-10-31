package com.foodai.backend.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodai.backend.profile.entity.UserProfileEntity;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
}
