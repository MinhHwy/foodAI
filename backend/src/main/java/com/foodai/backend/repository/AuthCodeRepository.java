package com.foodai.backend.repository;

import com.foodai.backend.entity.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {}
