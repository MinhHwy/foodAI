package com.foodai.backend.repository;

import com.foodai.backend.entity.ModelFoodClass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelFoodClassRepository extends JpaRepository<ModelFoodClass, Long> {
    Optional<ModelFoodClass> findByModelVersionAndClassLabel(String modelVersion, String classLabel);
}
