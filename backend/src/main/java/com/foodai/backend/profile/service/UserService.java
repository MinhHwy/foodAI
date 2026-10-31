package com.foodai.backend.profile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import com.foodai.backend.common.error.ApiException.BadRequestException;
import com.foodai.backend.common.time.UtcTimes;
import com.foodai.backend.profile.dto.GoalsUpdateRequest;
import com.foodai.backend.profile.dto.MeResponse;
import com.foodai.backend.profile.entity.UserEntity;
import com.foodai.backend.profile.entity.UserGoalEntity;
import com.foodai.backend.profile.entity.UserProfileEntity;
import com.foodai.backend.profile.entity.UserSettingsEntity;
import com.foodai.backend.profile.entity.WeightLogEntity;
import com.foodai.backend.profile.repository.UserGoalRepository;
import com.foodai.backend.profile.repository.UserProfileRepository;
import com.foodai.backend.profile.repository.UserRepository;
import com.foodai.backend.profile.repository.UserSettingsRepository;
import com.foodai.backend.profile.repository.WeightLogRepository;

/**
 * User profile service. All write paths derive the user id from the
 * authenticated SecurityContext (passed through by the controller), never from
 * the request body, so a user can only ever read/modify their own data.
 */
@Service
public class UserService {

	// Boundaries mirror the database CHECK constraints.
	public static final int MIN_AGE = 1;
	public static final int MAX_AGE = 120;
	public static final BigDecimal MIN_HEIGHT = new BigDecimal("30");
	public static final BigDecimal MAX_HEIGHT = new BigDecimal("300");
	public static final BigDecimal MIN_WEIGHT = new BigDecimal("1");
	public static final BigDecimal MAX_WEIGHT = new BigDecimal("600");
	public static final int MAX_DIETARY_NOTES = 1000;

	// Documented, configurable goal policy: Mifflin-St Jeor BMR, standard activity
	// multipliers, fixed daily adjustment per goal type with a safe floor for
	// losing weight, and a 30/45/25 protein/carbs/fat split (matches the frontend
	// sample where 2,000 kcal -> 150g / 225g / 55g).
	public static final BigDecimal CALORIE_DEFICIT_PER_DAY = new BigDecimal("500");
	public static final BigDecimal CALORIE_SURPLUS_PER_DAY = new BigDecimal("300");
	public static final BigDecimal MIN_LOSE_DAILY_CALORIES = new BigDecimal("1200");
	public static final BigDecimal PROTEIN_SHARE = new BigDecimal("0.30");
	public static final BigDecimal CARBS_SHARE = new BigDecimal("0.45");
	public static final BigDecimal FAT_SHARE = new BigDecimal("0.25");
	public static final String CALCULATION_METHOD = "mifflin-st-jeor (P30/C45/F25)";

	private static final BigDecimal ACTIVITY_MULTIPLIER_SEDENTARY = new BigDecimal("1.2");
	private static final BigDecimal ACTIVITY_MULTIPLIER_LIGHT = new BigDecimal("1.375");
	private static final BigDecimal ACTIVITY_MULTIPLIER_MODERATE = new BigDecimal("1.55");
	private static final BigDecimal ACTIVITY_MULTIPLIER_ACTIVE = new BigDecimal("1.725");
	private static final BigDecimal ACTIVITY_MULTIPLIER_VERY_ACTIVE = new BigDecimal("1.9");

	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;
	private final UserSettingsRepository userSettingsRepository;
	private final UserGoalRepository userGoalRepository;
	private final WeightLogRepository weightLogRepository;

	public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository,
			UserSettingsRepository userSettingsRepository, UserGoalRepository userGoalRepository,
			WeightLogRepository weightLogRepository) {
		this.userRepository = userRepository;
		this.userProfileRepository = userProfileRepository;
		this.userSettingsRepository = userSettingsRepository;
		this.userGoalRepository = userGoalRepository;
		this.weightLogRepository = weightLogRepository;
	}

	@Transactional
	public MeResponse getMe(Long userId) {
		UserEntity user = requireUser(userId);
		return buildMeResponse(user);
	}

	@Transactional
	public MeResponse patchProfile(Long userId, JsonNode body) {
		requireObjectBody(body, "PATCH /profile");
		UserEntity user = requireUser(userId);
		UserProfileEntity profile = ensureProfile(user);
		UserSettingsEntity settings = ensureSettings(user);

		if (body.has("fullName")) {
			String fullName = collectText(body, "fullName", 150);
			if (fullName == null || fullName.isBlank()) {
				throw new BadRequestException("fullName không được để trống.");
			}
			user.setFullName(fullName);
		}

		if (body.has("heightCm")) {
			BigDecimal height = collectDecimal(body, "heightCm");
			if (height != null && (height.compareTo(MIN_HEIGHT) < 0 || height.compareTo(MAX_HEIGHT) > 0)) {
				throw new BadRequestException("heightCm phải trong khoảng 30 ÷ 300 cm.");
			}
			profile.setHeightCm(height);
		}

		if (body.has("weightKg")) {
			applyWeightChange(profile, userId, body);
		}

		if (body.has("ageYears")) {
			Integer age = collectInteger(body, "ageYears");
			if (age != null && (age < MIN_AGE || age > MAX_AGE)) {
				throw new BadRequestException("ageYears phải trong khoảng 1 ÷ 120.");
			}
			profile.setAgeYears(age);
		}

		if (body.has("biologicalSex")) {
			UserProfileEntity.BiologicalSex sex = parseEnum(collectText(body, "biologicalSex", 20),
					"biologicalSex", UserProfileEntity.BiologicalSex.values());
			// The column is NOT NULL, so an explicit null cannot clear it.
			if (sex == null) {
				throw new BadRequestException(
						"biologicalSex nếu gửi thì phải là một trong: MALE, FEMALE, UNSPECIFIED.");
			}
			profile.setBiologicalSex(sex);
		}

		if (body.has("activityLevel")) {
			UserProfileEntity.ActivityLevel level = parseEnum(collectText(body, "activityLevel", 20),
					"activityLevel", UserProfileEntity.ActivityLevel.values());
			// Nullable column: explicit null clears the current activity level.
			profile.setActivityLevel(level);
		}

		if (body.has("dietaryNotes")) {
			applyDietaryNotes(settings, body);
		}

		return buildMeResponse(user);
	}

	@Transactional
	public MeResponse patchSettings(Long userId, JsonNode body) {
		requireObjectBody(body, "PATCH /settings");
		UserEntity user = requireUser(userId);
		UserSettingsEntity settings = ensureSettings(user);

		if (body.has("energyUnit")) {
			UserSettingsEntity.EnergyUnit unit = parseEnum(collectText(body, "energyUnit", 20),
					"energyUnit", UserSettingsEntity.EnergyUnit.values());
			if (unit == null) {
				throw new BadRequestException("energyUnit phải là một trong: KCAL, KJ.");
			}
			settings.setEnergyUnit(unit);
		}
		if (body.has("weightUnit")) {
			UserSettingsEntity.WeightUnit unit = parseEnum(collectText(body, "weightUnit", 20),
					"weightUnit", UserSettingsEntity.WeightUnit.values());
			if (unit == null) {
				throw new BadRequestException("weightUnit phải là một trong: KG, LB.");
			}
			settings.setWeightUnit(unit);
		}
		if (body.has("dailyRemindersEnabled")) {
			JsonNode value = body.get("dailyRemindersEnabled");
			if (!value.isBoolean()) {
				throw new BadRequestException("dailyRemindersEnabled phải là true hoặc false.");
			}
			settings.setDailyRemindersEnabled(value.asBoolean());
		}
		if (body.has("timezoneName")) {
			String zone = collectText(body, "timezoneName", 64);
			if (zone == null) {
				throw new BadRequestException("timezoneName không được để trống.");
			}
			try {
				java.time.ZoneId.of(zone);
			}
			catch (RuntimeException ex) {
				throw new BadRequestException("timezoneName phải là tên múi giờ hợp lệ, ví dụ Asia/Ho_Chi_Minh.");
			}
			settings.setTimezoneName(zone);
		}
		if (body.has("dietaryNotes")) {
			applyDietaryNotes(settings, body);
		}

		return buildMeResponse(user);
	}

	@Transactional
	public MeResponse putGoals(Long userId, GoalsUpdateRequest request) {
		UserEntity user = requireUser(userId);
		UserProfileEntity profile = ensureProfile(user);

		if (profile.getHeightCm() == null || profile.getCurrentWeightKg() == null
				|| profile.getAgeYears() == null) {
			throw new BadRequestException(
					"Hồ sơ sức khỏe chưa đủ dữ liệu (chiều cao, cân nặng, độ tuổi). Hãy PATCH /api/v1/me/profile trước.");
		}
		if (profile.getBiologicalSex() == null
				|| profile.getBiologicalSex() == UserProfileEntity.BiologicalSex.UNSPECIFIED) {
			throw new BadRequestException(
					"Cần giới tính sinh học (MALE/FEMALE) để tính BMR theo Mifflin-St Jeor. Hãy PATCH /api/v1/me/profile.");
		}

		BigDecimal bmr = calculateBmr(profile);
		BigDecimal tdee = calculateTdee(bmr, profile.getActivityLevel());
		BigDecimal dailyCalories = targetCalories(tdee, request.goalType());
		if (request.goalType() == UserGoalEntity.GoalType.LOSE
				&& dailyCalories.compareTo(MIN_LOSE_DAILY_CALORIES) < 0) {
			dailyCalories = MIN_LOSE_DAILY_CALORIES;
		}
		BigDecimal protein = dailyCalories.multiply(PROTEIN_SHARE).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
		BigDecimal carbs = dailyCalories.multiply(CARBS_SHARE).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
		BigDecimal fat = dailyCalories.multiply(FAT_SHARE).divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);

		LocalDate today = UtcTimes.today(UtcTimes.safeZone(currentSettingsZone(user.getId())));
		Optional<UserGoalEntity> openGoal = userGoalRepository
				.findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(user.getId());
		UserGoalEntity goal;
		if (openGoal.isPresent() && today.equals(openGoal.get().getEffectiveFrom())) {
			// Replacing a goal created earlier today updates the same row in place.
			goal = openGoal.get();
		}
		else {
			if (openGoal.isPresent()) {
				UserGoalEntity previous = openGoal.get();
				previous.setEffectiveTo(today);
				userGoalRepository.save(previous);
			}
			goal = new UserGoalEntity();
		}
		applyGoalValues(goal, userId, request, dailyCalories, bmr, tdee, protein, carbs, fat, today);
		userGoalRepository.save(goal);

		return buildMeResponse(user);
	}

	// ---------------------------------------------------------------- helpers

	private static void applyGoalValues(UserGoalEntity goal, Long userId, GoalsUpdateRequest request,
			BigDecimal dailyCalories, BigDecimal bmr, BigDecimal tdee, BigDecimal protein, BigDecimal carbs,
			BigDecimal fat, LocalDate today) {
		goal.setUserId(userId);
		goal.setGoalType(request.goalType());
		goal.setTargetWeightKg(request.targetWeightKg());
		goal.setDailyCaloriesKcal(dailyCalories.setScale(2, RoundingMode.HALF_UP));
		goal.setProteinTargetG(protein);
		goal.setCarbsTargetG(carbs);
		goal.setFatTargetG(fat);
		goal.setEstimatedBmrKcal(bmr.setScale(2, RoundingMode.HALF_UP));
		goal.setEstimatedTdeeKcal(tdee.setScale(2, RoundingMode.HALF_UP));
		goal.setCalculationMethod(CALCULATION_METHOD);
		goal.setEffectiveFrom(today);
		goal.setEffectiveTo(null); // open-ended until a future goal replaces it
	}

	private BigDecimal applyWeightChange(UserProfileEntity profile, Long userId, JsonNode body) {
		BigDecimal weight = collectDecimal(body, "weightKg");
		if (weight != null && (weight.compareTo(MIN_WEIGHT) < 0 || weight.compareTo(MAX_WEIGHT) > 0)) {
			throw new BadRequestException("weightKg phải trong khoảng 1 ÷ 600 kg.");
		}
		BigDecimal previous = profile.getCurrentWeightKg();
		boolean changed = weight == null ? previous != null
				: (previous == null || previous.compareTo(weight) != 0);
		if (!changed) {
			return null; // unchanged -> no weight log per contract
		}
		profile.setCurrentWeightKg(weight);
		if (weight == null) {
			return null; // clearing the weight is not a measurement
		}
		WeightLogEntity weightLog = new WeightLogEntity();
		weightLog.setUserId(userId);
		weightLog.setWeightKg(weight);
		weightLog.setMeasuredAtUtc(UtcTimes.nowUtc());
		weightLogRepository.save(weightLog);
		return weight;
	}

	private static void applyDietaryNotes(UserSettingsEntity settings, JsonNode body) {
		String notes = collectText(body, "dietaryNotes", MAX_DIETARY_NOTES);
		settings.setDietaryNotes(notes); // explicit null clears the notes
	}

	private UserEntity requireUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BadRequestException("Người dùng không tồn tại."));
	}

	private UserProfileEntity ensureProfile(UserEntity user) {
		Optional<UserProfileEntity> existing = userProfileRepository.findById(user.getId());
		return existing.orElseGet(() -> {
			UserProfileEntity profile = new UserProfileEntity();
			profile.setUserId(user.getId());
			profile.setBiologicalSex(UserProfileEntity.BiologicalSex.UNSPECIFIED);
			return userProfileRepository.save(profile);
		});
	}

	private UserSettingsEntity ensureSettings(UserEntity user) {
		return userSettingsRepository.findById(user.getId()).orElseGet(() -> {
			UserSettingsEntity settings = new UserSettingsEntity();
			settings.setUserId(user.getId());
			settings.setTimezoneName(UtcTimes.DEFAULT_ZONE.getId());
			settings.setEnergyUnit(UserSettingsEntity.EnergyUnit.KCAL);
			settings.setWeightUnit(UserSettingsEntity.WeightUnit.KG);
			settings.setDailyRemindersEnabled(Boolean.TRUE);
			return userSettingsRepository.save(settings);
		});
	}

	private String currentSettingsZone(Long userId) {
		return userSettingsRepository.findById(userId)
				.map(UserSettingsEntity::getTimezoneName)
				.orElse(UtcTimes.DEFAULT_ZONE.getId());
	}

	private static void requireObjectBody(JsonNode body, String endpoint) {
		if (body == null || !body.isObject()) {
			throw new BadRequestException("Body của " + endpoint + " phải là một JSON object.");
		}
	}

	private MeResponse buildMeResponse(UserEntity user) {
		Optional<UserGoalEntity> goal = userGoalRepository
				.findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(user.getId());
		return new MeResponse(MeResponse.UserDto.from(user), MeResponse.ProfileDto.from(ensureProfile(user)),
				MeResponse.SettingsDto.from(ensureSettings(user)), goal.map(MeResponse.GoalDto::from).orElse(null));
	}

	private BigDecimal calculateBmr(UserProfileEntity profile) {
		// Mifflin-St Jeor: 10*weight(kg) + 6.25*height(cm) - 5*age + (male ? +5 : -161)
		BigDecimal base = BigDecimal.TEN.multiply(profile.getCurrentWeightKg())
				.add(new BigDecimal("6.25").multiply(profile.getHeightCm()))
				.subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(profile.getAgeYears())));
		return profile.getBiologicalSex() == UserProfileEntity.BiologicalSex.MALE
				? base.add(BigDecimal.valueOf(5))
				: base.subtract(BigDecimal.valueOf(161));
	}

	private BigDecimal calculateTdee(BigDecimal bmr, UserProfileEntity.ActivityLevel activity) {
		BigDecimal multiplier = switch (activity == null ? UserProfileEntity.ActivityLevel.SEDENTARY : activity) {
			case SEDENTARY -> ACTIVITY_MULTIPLIER_SEDENTARY;
			case LIGHT -> ACTIVITY_MULTIPLIER_LIGHT;
			case MODERATE -> ACTIVITY_MULTIPLIER_MODERATE;
			case ACTIVE -> ACTIVITY_MULTIPLIER_ACTIVE;
			case VERY_ACTIVE -> ACTIVITY_MULTIPLIER_VERY_ACTIVE;
		};
		return bmr.multiply(multiplier);
	}

	private BigDecimal targetCalories(BigDecimal tdee, UserGoalEntity.GoalType goalType) {
		return switch (goalType) {
			case LOSE -> tdee.subtract(CALORIE_DEFICIT_PER_DAY);
			case MAINTAIN -> tdee;
			case GAIN -> tdee.add(CALORIE_SURPLUS_PER_DAY);
		};
	}

	private static String collectText(JsonNode body, String field, int maxLength) {
		JsonNode value = body.get(field);
		if (value == null || value.isNull()) {
			return null; // explicit null clears / not applicable
		}
		if (!value.isTextual()) {
			throw new BadRequestException("Trường '" + field + "' phải là chuỗi ký tự.");
		}
		String text = value.asText();
		if (text.length() > maxLength) {
			throw new BadRequestException("Trường '" + field + "' tối đa " + maxLength + " ký tự.");
		}
		return text;
	}

	private static BigDecimal collectDecimal(JsonNode body, String field) {
		JsonNode value = body.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		if (!value.isNumber()) {
			throw new BadRequestException("Trường '" + field + "' phải là số.");
		}
		return value.decimalValue();
	}

	private static Integer collectInteger(JsonNode body, String field) {
		JsonNode value = body.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		if (!value.isIntegralNumber()) {
			throw new BadRequestException("Trường '" + field + "' phải là số nguyên.");
		}
		return value.intValue();
	}

	private static <E extends Enum<E>> E parseEnum(String raw, String field, E[] allowed) {
		if (raw == null) {
			return null;
		}
		return Arrays.stream(allowed)
				.filter(constant -> constant.name().equalsIgnoreCase(raw))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Trường '" + field + "' phải thuộc một trong các giá trị: "
						+ String.join(", ", Arrays.stream(allowed).map(Enum::name).toList()) + "."));
	}
}
