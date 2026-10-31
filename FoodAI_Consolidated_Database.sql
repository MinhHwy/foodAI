-- FoodAI - Smart Food Recognition & Calories Tracking
-- Consolidated MySQL 8.0+ schema aligned with the web-based project proposal.
-- Stack: Spring Boot backend + MySQL; Python/YOLOv8 AI service communicates via API.
-- IMPORTANT: Demo seed rows are explicitly marked ESTIMATED/DEMO_UNVERIFIED.
-- Review this schema against the team's final FR list before production deployment.

CREATE DATABASE IF NOT EXISTS foodai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE foodai;

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- 1. USERS, AUTHENTICATION, PROFILE, SETTINGS AND GOALS
-- ============================================================

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL COMMENT 'Store BCrypt/Argon2 hash only; never plaintext',
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    account_status ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
    email_verified_at DATETIME(3) NULL,
    token_version INT NOT NULL DEFAULT 0 COMMENT 'Optional JWT invalidation version',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT chk_users_token_version CHECK (token_version >= 0)
) ENGINE=InnoDB;

CREATE TABLE auth_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code_hash CHAR(64) NOT NULL COMMENT 'Store a hash of verification/reset code, not raw code',
    purpose ENUM('VERIFY_EMAIL','RESET_PASSWORD') NOT NULL,
    attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    expires_at DATETIME(3) NOT NULL,
    consumed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_auth_user_purpose_expiry (user_id, purpose, expires_at),
    CONSTRAINT fk_auth_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_auth_attempts CHECK (attempts <= 10),
    CONSTRAINT chk_auth_expiration CHECK (expires_at > created_at)
) ENGINE=InnoDB;

CREATE TABLE user_profiles (
    user_id BIGINT NOT NULL,
    age_years SMALLINT UNSIGNED NULL COMMENT 'Age as requested by current UI; no invented birth date',
    biological_sex ENUM('MALE','FEMALE','UNSPECIFIED') NOT NULL DEFAULT 'UNSPECIFIED',
    height_cm DECIMAL(6,2) NULL,
    current_weight_kg DECIMAL(7,2) NULL,
    activity_level ENUM('SEDENTARY','LIGHT','MODERATE','ACTIVE','VERY_ACTIVE') NULL,
    avatar_url VARCHAR(1024) NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_profile_age CHECK (age_years IS NULL OR age_years BETWEEN 1 AND 120),
    CONSTRAINT chk_profile_height CHECK (height_cm IS NULL OR height_cm BETWEEN 30 AND 300),
    CONSTRAINT chk_profile_weight CHECK (current_weight_kg IS NULL OR current_weight_kg BETWEEN 1 AND 600)
) ENGINE=InnoDB;

CREATE TABLE user_settings (
    user_id BIGINT NOT NULL,
    timezone_name VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    energy_unit ENUM('KCAL','KJ') NOT NULL DEFAULT 'KCAL',
    weight_unit ENUM('KG','LB') NOT NULL DEFAULT 'KG',
    daily_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    dietary_notes VARCHAR(1000) NULL COMMENT 'Free-text preference only; not a verified allergy list',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_goals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goal_type ENUM('LOSE','MAINTAIN','GAIN') NOT NULL,
    target_weight_kg DECIMAL(7,2) NULL,
    daily_calories_kcal DECIMAL(9,2) NOT NULL,
    protein_target_g DECIMAL(9,2) NOT NULL,
    carbs_target_g DECIMAL(9,2) NOT NULL,
    fat_target_g DECIMAL(9,2) NOT NULL,
    estimated_bmr_kcal DECIMAL(9,2) NULL,
    estimated_tdee_kcal DECIMAL(9,2) NULL,
    calculation_method VARCHAR(80) NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL COMMENT 'Exclusive end date; NULL means currently open-ended',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_user_goals_period (user_id, effective_from, effective_to),
    CONSTRAINT fk_user_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_goal_target_weight CHECK (target_weight_kg IS NULL OR target_weight_kg > 0),
    CONSTRAINT chk_goal_calories CHECK (daily_calories_kcal > 0),
    CONSTRAINT chk_goal_protein CHECK (protein_target_g >= 0),
    CONSTRAINT chk_goal_carbs CHECK (carbs_target_g >= 0),
    CONSTRAINT chk_goal_fat CHECK (fat_target_g >= 0),
    CONSTRAINT chk_goal_bmr CHECK (estimated_bmr_kcal IS NULL OR estimated_bmr_kcal > 0),
    CONSTRAINT chk_goal_tdee CHECK (estimated_tdee_kcal IS NULL OR estimated_tdee_kcal > 0),
    CONSTRAINT chk_goal_period CHECK (effective_to IS NULL OR effective_to > effective_from)
) ENGINE=InnoDB;

CREATE TABLE weight_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    weight_kg DECIMAL(7,2) NOT NULL,
    measured_at_utc DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_weight_logs_user_time (user_id, measured_at_utc),
    CONSTRAINT fk_weight_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_weight_logs_positive CHECK (weight_kg > 0)
) ENGINE=InnoDB;

-- ============================================================
-- 2. FOOD CATALOG AND YOLO CLASS MAPPING
-- Nutrition is standardized PER 100 GRAMS to avoid two competing
-- sources of truth. Backend computes portion nutrition from grams.
-- ============================================================

CREATE TABLE food_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_food_categories_name_vi (name_vi)
) ENGINE=InnoDB;

CREATE TABLE foods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NULL,
    name_vi VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NULL,
    region_vi VARCHAR(120) NULL,
    description TEXT NULL,
    ingredients_summary TEXT NULL,
    image_url VARCHAR(1024) NULL,
    standard_serving_label VARCHAR(80) NOT NULL DEFAULT '1 phần',
    standard_serving_grams DECIMAL(9,2) NULL COMMENT 'Reference portion; NULL if unknown',
    calories_per_100g_kcal DECIMAL(8,2) NOT NULL,
    protein_per_100g_g DECIMAL(8,2) NOT NULL,
    carbs_per_100g_g DECIMAL(8,2) NOT NULL,
    fat_per_100g_g DECIMAL(8,2) NOT NULL,
    data_quality ENUM('DEMO_UNVERIFIED','ESTIMATED','VERIFIED') NOT NULL DEFAULT 'DEMO_UNVERIFIED',
    catalog_status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    nutrition_source VARCHAR(500) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_foods_name_vi (name_vi),
    KEY idx_foods_category_status (category_id, catalog_status),
    CONSTRAINT fk_foods_category FOREIGN KEY (category_id) REFERENCES food_categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_foods_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_foods_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_foods_serving_grams CHECK (standard_serving_grams IS NULL OR standard_serving_grams > 0),
    CONSTRAINT chk_foods_calories CHECK (calories_per_100g_kcal >= 0),
    CONSTRAINT chk_foods_protein CHECK (protein_per_100g_g >= 0),
    CONSTRAINT chk_foods_carbs CHECK (carbs_per_100g_g >= 0),
    CONSTRAINT chk_foods_fat CHECK (fat_per_100g_g >= 0)
) ENGINE=InnoDB;

CREATE TABLE food_aliases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    food_id BIGINT NOT NULL,
    alias_name VARCHAR(200) NOT NULL,
    language_tag VARCHAR(12) NOT NULL DEFAULT 'vi',
    PRIMARY KEY (id),
    UNIQUE KEY uq_food_alias_language (food_id, alias_name, language_tag),
    KEY idx_food_aliases_search (alias_name),
    CONSTRAINT fk_food_aliases_food FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE model_food_classes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    model_version VARCHAR(100) NOT NULL,
    class_label VARCHAR(200) NOT NULL,
    food_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_model_class_version_label (model_version, class_label),
    KEY idx_model_food_classes_food (food_id),
    CONSTRAINT fk_model_food_classes_food FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- 3. IMAGE RECOGNITION, PORTION ESTIMATION AND USER CORRECTIONS
-- ============================================================

CREATE TABLE recognition_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    image_url VARCHAR(1024) NOT NULL COMMENT 'Cloudinary secure URL',
    cloudinary_public_id VARCHAR(255) NULL,
    mime_type VARCHAR(100) NULL,
    file_size_bytes BIGINT NULL,
    status ENUM('UPLOADED','PROCESSING','COMPLETED','REJECTED','FAILED') NOT NULL DEFAULT 'UPLOADED',
    rejection_reason VARCHAR(500) NULL,
    model_version VARCHAR(100) NULL,
    error_code VARCHAR(100) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_recognition_sessions_user_created (user_id, created_at),
    KEY idx_recognition_sessions_status_created (status, created_at),
    CONSTRAINT fk_recognition_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_recognition_file_size CHECK (file_size_bytes IS NULL OR file_size_bytes BETWEEN 1 AND 10485760),
    CONSTRAINT chk_recognition_finished CHECK (finished_at IS NULL OR finished_at >= created_at)
) ENGINE=InnoDB;

CREATE TABLE recognition_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    predicted_food_id BIGINT NULL,
    predicted_label VARCHAR(200) NULL COMMENT 'Keep raw AI label even if mapping is unavailable',
    selected_food_id BIGINT NULL COMMENT 'Food confirmed/selected by user',
    confidence_score DECIMAL(6,5) NULL,
    bbox_x DECIMAL(7,6) NULL COMMENT 'Normalized coordinate 0..1',
    bbox_y DECIMAL(7,6) NULL,
    bbox_width DECIMAL(7,6) NULL,
    bbox_height DECIMAL(7,6) NULL,
    detection_metadata JSON NULL COMMENT 'Optional AI metadata; avoid storing image bytes here',
    estimated_grams DECIMAL(9,2) NULL,
    portion_multiplier DECIMAL(9,3) NULL COMMENT 'Estimated number of standard servings, if available',
    portion_method ENUM('AI_ESTIMATE','REFERENCE_OBJECT','MANUAL','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    portion_confidence DECIMAL(6,5) NULL,
    explanation VARCHAR(1000) NULL,
    item_status ENUM('PREDICTED','CONFIRMED','REJECTED') NOT NULL DEFAULT 'PREDICTED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_recognition_items_session (session_id),
    KEY idx_recognition_items_predicted_food (predicted_food_id),
    KEY idx_recognition_items_selected_food (selected_food_id),
    CONSTRAINT fk_recognition_items_session FOREIGN KEY (session_id) REFERENCES recognition_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_recognition_items_predicted_food FOREIGN KEY (predicted_food_id) REFERENCES foods(id) ON DELETE SET NULL,
    CONSTRAINT fk_recognition_items_selected_food FOREIGN KEY (selected_food_id) REFERENCES foods(id) ON DELETE SET NULL,
    CONSTRAINT chk_recognition_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 1),
    CONSTRAINT chk_recognition_portion_confidence CHECK (portion_confidence IS NULL OR portion_confidence BETWEEN 0 AND 1),
    CONSTRAINT chk_recognition_portion_multiplier CHECK (portion_multiplier IS NULL OR portion_multiplier > 0),
    CONSTRAINT chk_recognition_estimated_grams CHECK (estimated_grams IS NULL OR estimated_grams > 0),
    CONSTRAINT chk_recognition_bbox_x CHECK (bbox_x IS NULL OR bbox_x BETWEEN 0 AND 1),
    CONSTRAINT chk_recognition_bbox_y CHECK (bbox_y IS NULL OR bbox_y BETWEEN 0 AND 1),
    CONSTRAINT chk_recognition_bbox_width CHECK (bbox_width IS NULL OR (bbox_width > 0 AND bbox_width <= 1)),
    CONSTRAINT chk_recognition_bbox_height CHECK (bbox_height IS NULL OR (bbox_height > 0 AND bbox_height <= 1)),
    CONSTRAINT chk_recognition_bbox_right CHECK (bbox_x IS NULL OR bbox_width IS NULL OR bbox_x + bbox_width <= 1),
    CONSTRAINT chk_recognition_bbox_bottom CHECK (bbox_y IS NULL OR bbox_height IS NULL OR bbox_y + bbox_height <= 1)
) ENGINE=InnoDB;

CREATE TABLE recognition_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recognition_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    previous_food_id BIGINT NULL,
    corrected_food_id BIGINT NULL,
    previous_portion_multiplier DECIMAL(9,3) NULL,
    corrected_portion_multiplier DECIMAL(9,3) NULL,
    correction_note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_recognition_feedback_item_created (recognition_item_id, created_at),
    KEY idx_recognition_feedback_user (user_id, created_at),
    CONSTRAINT fk_recognition_feedback_item FOREIGN KEY (recognition_item_id) REFERENCES recognition_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_recognition_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recognition_feedback_previous_food FOREIGN KEY (previous_food_id) REFERENCES foods(id) ON DELETE SET NULL,
    CONSTRAINT fk_recognition_feedback_corrected_food FOREIGN KEY (corrected_food_id) REFERENCES foods(id) ON DELETE SET NULL,
    CONSTRAINT chk_feedback_previous_portion CHECK (previous_portion_multiplier IS NULL OR previous_portion_multiplier > 0),
    CONSTRAINT chk_feedback_corrected_portion CHECK (corrected_portion_multiplier IS NULL OR corrected_portion_multiplier > 0)
) ENGINE=InnoDB;

-- ============================================================
-- 4. FOOD DIARY
-- Diary item nutrition values are snapshots so later food catalog
-- edits cannot silently rewrite historical meals.
-- Totals are calculated from diary_items (no duplicated totals).
-- ============================================================

CREATE TABLE diary_meals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    meal_type ENUM('BREAKFAST','LUNCH','DINNER','SNACK') NOT NULL,
    eaten_at_utc DATETIME(3) NOT NULL,
    local_meal_date DATE NOT NULL COMMENT 'Computed by backend using the user timezone',
    source_type ENUM('MANUAL','RECOGNITION','RECOMMENDATION') NOT NULL DEFAULT 'MANUAL',
    notes VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_diary_meals_user_date (user_id, local_meal_date, meal_type),
    KEY idx_diary_meals_user_time (user_id, eaten_at_utc),
    CONSTRAINT fk_diary_meals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE diary_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meal_id BIGINT NOT NULL,
    food_id BIGINT NULL,
    recognition_item_id BIGINT NULL,
    food_name_snapshot VARCHAR(200) NOT NULL,
    serving_label_snapshot VARCHAR(80) NULL,
    portion_multiplier DECIMAL(9,3) NOT NULL DEFAULT 1.000,
    consumed_grams DECIMAL(9,2) NULL,
    calories_kcal DECIMAL(10,2) NOT NULL,
    protein_g DECIMAL(10,2) NOT NULL,
    carbs_g DECIMAL(10,2) NOT NULL,
    fat_g DECIMAL(10,2) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_diary_items_meal (meal_id),
    KEY idx_diary_items_food (food_id),
    KEY idx_diary_items_recognition_item (recognition_item_id),
    CONSTRAINT fk_diary_items_meal FOREIGN KEY (meal_id) REFERENCES diary_meals(id) ON DELETE CASCADE,
    CONSTRAINT fk_diary_items_food FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE SET NULL,
    CONSTRAINT fk_diary_items_recognition_item FOREIGN KEY (recognition_item_id) REFERENCES recognition_items(id) ON DELETE SET NULL,
    CONSTRAINT chk_diary_items_portion CHECK (portion_multiplier > 0),
    CONSTRAINT chk_diary_items_grams CHECK (consumed_grams IS NULL OR consumed_grams > 0),
    CONSTRAINT chk_diary_items_calories CHECK (calories_kcal >= 0),
    CONSTRAINT chk_diary_items_protein CHECK (protein_g >= 0),
    CONSTRAINT chk_diary_items_carbs CHECK (carbs_g >= 0),
    CONSTRAINT chk_diary_items_fat CHECK (fat_g >= 0)
) ENGINE=InnoDB;

-- ============================================================
-- 5. RULE/SCORE-BASED RECOMMENDATIONS AND AUDIT
-- ============================================================

CREATE TABLE recommendation_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goal_id BIGINT NULL,
    target_date DATE NOT NULL,
    target_meal_type ENUM('BREAKFAST','LUNCH','DINNER','SNACK') NULL,
    remaining_calories_kcal DECIMAL(10,2) NULL,
    algorithm_version VARCHAR(80) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_recommendation_runs_user_date (user_id, target_date),
    KEY idx_recommendation_runs_goal (goal_id),
    CONSTRAINT fk_recommendation_runs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_runs_goal FOREIGN KEY (goal_id) REFERENCES user_goals(id) ON DELETE SET NULL,
    CONSTRAINT chk_recommendation_remaining_calories CHECK (remaining_calories_kcal IS NULL OR remaining_calories_kcal >= 0)
) ENGINE=InnoDB;

CREATE TABLE recommendation_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    food_id BIGINT NULL,
    rank_position SMALLINT UNSIGNED NOT NULL,
    relevance_score DECIMAL(7,5) NULL,
    reason_text VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_recommendation_run_rank (run_id, rank_position),
    KEY idx_recommendation_items_food (food_id),
    CONSTRAINT fk_recommendation_items_run FOREIGN KEY (run_id) REFERENCES recommendation_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_items_food FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE SET NULL,
    CONSTRAINT chk_recommendation_rank CHECK (rank_position > 0),
    CONSTRAINT chk_recommendation_score CHECK (relevance_score IS NULL OR relevance_score BETWEEN 0 AND 1)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    actor_email VARCHAR(190) NULL COMMENT 'Optional audit snapshot; do not use as login identity',
    action_code VARCHAR(100) NOT NULL,
    object_type VARCHAR(80) NULL,
    object_id BIGINT NULL,
    outcome ENUM('SUCCESS','FAILURE') NOT NULL DEFAULT 'SUCCESS',
    details_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_actor_created (actor_user_id, created_at),
    KEY idx_audit_action_created (action_code, created_at),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 6. OPTIONAL STARTER DATA
-- Values are deliberately marked ESTIMATED / DRAFT until verified
-- against a trusted nutrition source by the project team.
-- ============================================================

INSERT INTO food_categories (name_vi, name_en) VALUES
    ('Món nước', 'Noodle soups'),
    ('Món chính', 'Main dishes'),
    ('Salad & khai vị', 'Salads & starters'),
    ('Món cuốn', 'Fresh rolls');

-- Example foods are not authoritative nutrition advice/data.
INSERT INTO foods (
    category_id, name_vi, name_en, region_vi, standard_serving_label,
    standard_serving_grams, calories_per_100g_kcal, protein_per_100g_g,
    carbs_per_100g_g, fat_per_100g_g, data_quality, catalog_status, nutrition_source
) VALUES
    (1, 'Phở gà', 'Chicken pho', 'Việt Nam', '1 tô vừa', 500.00,
     84.00, 7.60, 10.40, 2.40, 'ESTIMATED', 'DRAFT', 'Placeholder estimate - verify before publishing'),
    (2, 'Cơm tấm sườn', 'Broken rice with pork chop', 'Miền Nam', '1 đĩa vừa', 400.00,
     155.00, 8.00, 19.50, 4.50, 'ESTIMATED', 'DRAFT', 'Placeholder estimate - verify before publishing'),
    (1, 'Bún bò Huế', 'Hue-style beef noodle soup', 'Miền Trung', '1 tô vừa', 550.00,
     94.55, 5.45, 10.55, 2.91, 'ESTIMATED', 'DRAFT', 'Placeholder estimate - verify before publishing');
