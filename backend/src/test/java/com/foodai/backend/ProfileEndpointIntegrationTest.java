package com.foodai.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.foodai.backend.profile.entity.UserEntity;
import com.foodai.backend.profile.entity.WeightLogEntity;
import com.foodai.backend.profile.repository.UserGoalRepository;
import com.foodai.backend.profile.repository.UserProfileRepository;
import com.foodai.backend.profile.repository.UserRepository;
import com.foodai.backend.profile.repository.UserSettingsRepository;
import com.foodai.backend.profile.repository.WeightLogRepository;
import com.foodai.backend.profile.service.UserService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "foodai.jwt.secret=" + TestSupport.JWT_SECRET)
@AutoConfigureMockMvc
@Transactional
class ProfileEndpointIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserProfileRepository profileRepository;

	@Autowired
	UserSettingsRepository settingsRepository;

	@Autowired
	UserGoalRepository goalRepository;

	@Autowired
	WeightLogRepository weightLogRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	// ------------------------------------------------------ shared test helpers

	private String registerAndLogin() throws Exception {
		return registerAndLogin(TestSupport.uniqueEmail());
	}

	private String registerAndLogin(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content("{\"fullName\":\"Minh Anh\",\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isCreated());
		String loginBody = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(loginBody).get("accessToken").asString();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private Long currentUserId(String token) throws Exception {
		String body = mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).path("user").get("id").asLong();
	}

	private void patchProfile(String token, String body) throws Exception {
		mockMvc.perform(patch("/api/v1/me/profile").header("Authorization", bearer(token))
				.contentType("application/json").content(body)).andExpect(status().isOk());
	}

	private void seedHeightWeightAge(String token, String height, String weight, Integer age, String sex,
			String activity) throws Exception {
		patchProfile(token, ("{\"heightCm\":%s,\"weightKg\":%s,\"ageYears\":%d,\"biologicalSex\":\"%s\","
				+ "\"activityLevel\":\"%s\"}").formatted(height, weight, age, sex, activity));
	}

	private UserEntity seedUser(String email) {
		UserEntity user = new UserEntity();
		user.setFullName("Minh Anh");
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode("Passw0rd123"));
		user.setRole(UserEntity.UserRole.USER);
		user.setAccountStatus(UserEntity.AccountStatus.ACTIVE);
		user.setEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
		return userRepository.save(user);
	}

	private long weightLoggedCount(Long userId) {
		return weightLogRepository.findAll().stream()
				.map(WeightLogEntity.class::cast)
				.filter(log -> log.getUserId().equals(userId))
				.count();
	}

	private BigDecimal bmrOf(int weight, double height, int age, boolean male) {
		// mirrors the service formula for an independent numeric check
		return BigDecimal.valueOf(10 * weight + 6.25 * height - 5.0 * age + (male ? 5 : -161))
				.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal tdeeOf(BigDecimal bmr, String multiplier) {
		return bmr.multiply(new BigDecimal(multiplier)).setScale(2, RoundingMode.HALF_UP);
	}

	// ------------------------------------------------------------ GET /api/v1/me

	@Test
	void meIsRejectedWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value(
						"Yêu cầu cần xác thực. Hãy đăng nhập và gửi kèm header 'Authorization: Bearer <token>'."));
	}

	@Test
	void meIsRejectedWithMalformedOrGarbageToken() throws Exception {
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Basic dXNlcjpwYXNz"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meIsRejectedWithExpiredToken() throws Exception {
		String email = TestSupport.uniqueEmail();
		UserEntity user = seedUser(email);
		String token = TestSupport.expiredToken(user.getId(), user.getEmail(), user.getTokenVersion());
		mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meIsRejectedAfterTokenVersionRotation() throws Exception {
		String email = TestSupport.uniqueEmail();
		seedUser(email);
		UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		String token = TestSupport.validToken(user.getId(), user.getEmail(), user.getTokenVersion());
		user.incrementTokenVersion();
		userRepository.save(user);
		mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsProfileSettingsAndNoSensitiveFields() throws Exception {
		String token = registerAndLogin();
		String body = mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.id").isNumber())
				.andExpect(jsonPath("$.user.fullName").value("Minh Anh"))
				.andExpect(jsonPath("$.profile.biologicalSex").value("UNSPECIFIED"))
				.andExpect(jsonPath("$.settings.energyUnit").value("KCAL"))
				.andExpect(jsonPath("$.settings.weightUnit").value("KG"))
				.andExpect(jsonPath("$.settings.dailyRemindersEnabled").value(true))
				.andExpect(jsonPath("$.goals").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain("password").doesNotContain("passwordHash")
				.doesNotContain("tokenVersion").doesNotContain("accessToken");
	}

	// ------------------------------------------------- PATCH /api/v1/me/profile

	@Test
	void patchProfileUpdatesOnlySentFields() throws Exception {
		String token = registerAndLogin();
		Long userId = currentUserId(token);
		// only fullName is sent; every other field must stay at its default
		patchProfile(token, "{\"fullName\":\"Bui Minh Anh\"}");
		UserEntity user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getFullName()).isEqualTo("Bui Minh Anh");
		mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.user.email").isNotEmpty())
				.andExpect(jsonPath("$.profile.biologicalSex").value("UNSPECIFIED"))
				.andExpect(jsonPath("$.profile.heightCm").doesNotExist());
	}

	@Test
	void patchProfileChangesWeightAndWritesWeightLog() throws Exception {
		String token = registerAndLogin();
		Long userId = currentUserId(token);
		patchProfile(token, "{\"heightCm\":170,\"weightKg\":65.5,\"ageYears\":21,\"biologicalSex\":\"MALE\","
				+ "\"activityLevel\":\"LIGHT\"}");
		assertThat(profileRepository.findById(userId).orElseThrow().getCurrentWeightKg())
				.isEqualByComparingTo("65.50");
		assertThat(weightLoggedCount(userId)).isEqualTo(1);
		// sending the same weight again must NOT create another weight log entry
		patchProfile(token, "{\"weightKg\":65.5}");
		assertThat(weightLoggedCount(userId)).isEqualTo(1);
		// changing the weight again adds exactly one more entry
		patchProfile(token, "{\"weightKg\":66}");
		assertThat(weightLoggedCount(userId)).isEqualTo(2);
	}

	@Test
	void patchProfileRejectsInvalidValues() throws Exception {
		String token = registerAndLogin();
		assertPatchProfile400(token, "{\"heightCm\":301}", "heightCm");
		assertPatchProfile400(token, "{\"heightCm\":29}", "heightCm");
		assertPatchProfile400(token, "{\"weightKg\":0.5}", "weightKg");
		assertPatchProfile400(token, "{\"weightKg\":601}", "weightKg");
		assertPatchProfile400(token, "{\"ageYears\":0}", "ageYears");
		assertPatchProfile400(token, "{\"ageYears\":121}", "ageYears");
		assertPatchProfile400(token, "{\"ageYears\":21.5}", "số nguyên");
		assertPatchProfile400(token, "{\"biologicalSex\":\"banana\"}", "biologicalSex");
		assertPatchProfile400(token, "{\"activityLevel\":\"ALWAYS\"}", "activityLevel");
		assertPatchProfile400(token, "{\"fullName\":\"\"}", "fullName");
		assertPatchProfile400(token, "{\"weightKg\":\"seventy\"}", "weightKg");
		assertPatchProfile400(token, "[1,2,3]", "PATCH /profile");
		// unknown fields must be ignored, not rejected
		patchProfile(token, "{\"unknownField\":\"x\",\"role\":\"ADMIN\",\"userId\":9999}");
	}

	@Test
	void patchProfileAcceptsNullableClearing() throws Exception {
		String token = registerAndLogin();
		patchProfile(token, "{\"heightCm\":170,\"dietaryNotes\":\"Không ăn hải sản\"}");
		mockMvc.perform(patch("/api/v1/me/profile").header("Authorization", bearer(token))
				.contentType("application/json")
				.content("{\"heightCm\":null,\"dietaryNotes\":null,\"activityLevel\":null}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profile.heightCm").doesNotExist())
				.andExpect(jsonPath("$.settings.dietaryNotes").doesNotExist())
				.andExpect(jsonPath("$.profile.activityLevel").doesNotExist());
	}

	// ------------------------------------------------ PATCH /api/v1/me/settings

	@Test
	void patchSettingsUpdatesUnitsAndReminders() throws Exception {
		String token = registerAndLogin();
		mockMvc.perform(patch("/api/v1/me/settings").header("Authorization", bearer(token))
				.contentType("application/json")
				.content("{\"energyUnit\":\"KJ\",\"weightUnit\":\"LB\",\"dailyRemindersEnabled\":false,"
						+ "\"dietaryNotes\":\"Chay\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settings.energyUnit").value("KJ"))
				.andExpect(jsonPath("$.settings.weightUnit").value("LB"))
				.andExpect(jsonPath("$.settings.dailyRemindersEnabled").value(false))
				.andExpect(jsonPath("$.settings.dietaryNotes").value("Chay"));
		assertPatchSettings400(token, "{\"energyUnit\":\"JOULE\"}", "energyUnit");
		assertPatchSettings400(token, "{\"weightUnit\":\"STONE\"}", "weightUnit");
		assertPatchSettings400(token, "{\"dailyRemindersEnabled\":\"yes\"}", "dailyRemindersEnabled");
		assertPatchSettings400(token, "{\"timezoneName\":\"Mars/Olympus_Mons\"}", "timezoneName");
	}

	// --------------------------------------------------- PUT /api/v1/me/goals

	@Test
	void putGoalsRequiresCompleteHealthProfile() throws Exception {
		String token = registerAndLogin();
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"goalType\":\"LOSE\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"Hồ sơ sức khỏe chưa đủ dữ liệu (chiều cao, cân nặng, độ tuổi). Hãy PATCH /api/v1/me/profile trước."));
	}

	@Test
	void putGoalsRequiresBiologicalSexForBmr() throws Exception {
		String token = registerAndLogin();
		patchProfile(token, "{\"heightCm\":170,\"weightKg\":65,\"ageYears\":21}");
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"goalType\":\"MAINTAIN\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("giới tính sinh học")));
	}

	@Test
	void putGoalsComputesMifflinStJeorTargets() throws Exception {
		String token = registerAndLogin();
		seedHeightWeightAge(token, "170", "65", 21, "MALE", "LIGHT");
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json")
				.content("{\"goalType\":\"LOSE\",\"targetWeightKg\":60}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.goals.goalType").value("LOSE"))
				.andExpect(jsonPath("$.goals.calculationMethod").value(UserService.CALCULATION_METHOD))
				.andExpect(jsonPath("$.goals.effectiveFrom").value(java.time.LocalDate.now(
						java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString()));
		// independent numeric verification
		BigDecimal bmr = bmrOf(65, 170, 21, true);
		BigDecimal tdee = tdeeOf(bmr, "1.375");
		BigDecimal daily = tdee.subtract(UserService.CALORIE_DEFICIT_PER_DAY);
		BigDecimal protein = daily.multiply(UserService.PROTEIN_SHARE).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
		BigDecimal carbs = daily.multiply(UserService.CARBS_SHARE).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
		BigDecimal fat = daily.multiply(UserService.FAT_SHARE).divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);
		String body = mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andReturn().getResponse().getContentAsString();
		var goals = objectMapper.readTree(body).get("goals");
		assertThat(goals.get("estimatedBmrKcal").decimalValue()).isEqualByComparingTo(bmr);
		assertThat(goals.get("estimatedTdeeKcal").decimalValue()).isEqualByComparingTo(tdee);
		assertThat(goals.get("dailyCaloriesKcal").decimalValue()).isEqualByComparingTo(daily);
		assertThat(goals.get("proteinTargetG").decimalValue()).isEqualByComparingTo(protein);
		assertThat(goals.get("carbsTargetG").decimalValue()).isEqualByComparingTo(carbs);
		assertThat(goals.get("fatTargetG").decimalValue()).isEqualByComparingTo(fat);
		assertThat(goals.get("targetWeightKg").decimalValue()).isEqualByComparingTo("60");
	}

	@Test
	void putGoalsMaintainAndGainFollowPolicyAndReplaceSameDayGoal() throws Exception {
		String token = registerAndLogin();
		Long userId = currentUserId(token);
		seedHeightWeightAge(token, "170", "65", 21, "MALE", "SEDENTARY");
		BigDecimal tdee = tdeeOf(bmrOf(65, 170, 21, true), "1.2");
		// MAINTAIN == TDEE
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"goalType\":\"MAINTAIN\"}"))
				.andExpect(status().isOk());
		// replaced same-day: only one open-ended goal row remains
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"goalType\":\"GAIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.goals.goalType").value("GAIN"));
		String body = mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
				.andReturn().getResponse().getContentAsString();
		var goals = objectMapper.readTree(body).get("goals");
		assertThat(goals.get("dailyCaloriesKcal").decimalValue()).isEqualByComparingTo(
				tdee.add(UserService.CALORIE_SURPLUS_PER_DAY));
		assertThat(goalRepository.findAll().stream()
				.filter(goal -> goal.getUserId().equals(userId) && goal.getEffectiveTo() == null).count())
				.isEqualTo(1);
	}

	@Test
	void putGoalsLoseFloorsAtSafeMinimum() throws Exception {
		String token = registerAndLogin();
		// small frame: 150cm, 40kg, 60yo male, SEDENTARY -> TDEE 1491.00; LOSE 991 -> floored to 1200
		seedHeightWeightAge(token, "150", "40", 60, "MALE", "SEDENTARY");
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"goalType\":\"LOSE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.goals.dailyCaloriesKcal").value(1200.0));
	}

	@Test
	void putGoalsRejectsUnknownGoalType() throws Exception {
		String token = registerAndLogin();
		seedHeightWeightAge(token, "170", "65", 21, "FEMALE", "ACTIVE");
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"goalType\":\"BULK_HARD\"}"))
				.andExpect(status().isBadRequest());
		// goalType is mandatory
		mockMvc.perform(put("/api/v1/me/goals").header("Authorization", bearer(token))
				.contentType("application/json").content("{\"targetWeightKg\":60}"))
				.andExpect(status().isBadRequest());
	}

	// --------------------------------------------- ownership isolation checks

	@Test
	void userCannotReadOrModifyAnotherUsersProfile() throws Exception {
		String tokenA = registerAndLogin();
		Long idA = currentUserId(tokenA);
		String emailB = TestSupport.uniqueEmail();
		String tokenB = registerAndLogin(emailB);
		Long idB = currentUserId(tokenB);
		assertThat(idA).isNotEqualTo(idB);
		// A updates own profile; B's rows must remain untouched
		patchProfile(tokenA, "{\"weightKg\":55,\"fullName\":\"User A\"}");
		assertThat(userRepository.findById(idB).orElseThrow().getFullName()).isEqualTo("Minh Anh");
		assertThat(profileRepository.findById(idB).orElseThrow().getCurrentWeightKg()).isNull();
		assertThat(weightLoggedCount(idB)).isEqualTo(0);
		// userId in the payload is ignored: the write targets the token owner only
		patchProfile(tokenA, "{\"userId\":" + idB + ",\"fullName\":\"Impersonated\"}");
		assertThat(userRepository.findById(idB).orElseThrow().getFullName()).isEqualTo("Minh Anh");
		assertThat(userRepository.findById(idA).orElseThrow().getFullName()).isEqualTo("Impersonated");
		// and B still reads their own (unmodified) data with their own token
		mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(tokenB)))
				.andExpect(jsonPath("$.user.fullName").value("Minh Anh"))
				.andExpect(jsonPath("$.user.id").value(idB.intValue()));
	}

	// ---------------------------------------------------------------- helpers

	private void assertPatchProfile400(String token, String body, String detailFragment) throws Exception {
		mockMvc.perform(patch("/api/v1/me/profile").header("Authorization", bearer(token))
				.contentType("application/json").content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(detailFragment)));
	}

	private void assertPatchSettings400(String token, String body, String detailFragment) throws Exception {
		mockMvc.perform(patch("/api/v1/me/settings").header("Authorization", bearer(token))
				.contentType("application/json").content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(detailFragment)));
	}
}
