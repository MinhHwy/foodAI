package com.foodai.backend.repository;

import com.foodai.backend.entity.FoodAlias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodAliasRepository extends JpaRepository<FoodAlias, Long> {}
