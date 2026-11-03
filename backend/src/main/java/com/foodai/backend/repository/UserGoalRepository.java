package com.foodai.backend.repository;

import com.foodai.backend.entity.UserGoal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {
	Optional<UserGoal> findFirstByUser_IdAndEffectiveToIsNullOrderByEffectiveFromDesc(Long userId);
}
