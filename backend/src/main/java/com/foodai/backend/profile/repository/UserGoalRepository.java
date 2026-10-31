package com.foodai.backend.profile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodai.backend.profile.entity.UserGoalEntity;

public interface UserGoalRepository extends JpaRepository<UserGoalEntity, Long> {

	Optional<UserGoalEntity> findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(Long userId);
}
