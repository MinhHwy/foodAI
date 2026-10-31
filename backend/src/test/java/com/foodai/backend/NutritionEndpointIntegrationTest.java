package com.foodai.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.foodai.backend.food.entity.FoodEntity;
import com.foodai.backend.food.repository.FoodRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "foodai.jwt.secret=" + TestSupport.JWT_SECRET)
@AutoConfigureMockMvc
@Transactional
class NutritionEndpointIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	FoodRepository foodRepository;

	// ------------------------------------------------------ shared test helpers

	private String bearerToken() throws Exception {
		String email = TestSupport.uniqueEmail();
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content("{\"fullName\":\"Minh Anh\",\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isCreated());
		String loginBody = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(loginBody).get("accessToken").asString();
	}

	private FoodEntity insertFood(String name, String calories, String protein, String carbs, String fat,
			String servingGrams) {
		FoodEntity food = new FoodEntity();
		food.setNameVi(name);
		food.setStandardServingLabel("1 phần");
		food.setStandardServingGrams(servingGrams == null ? null : new BigDecimal(servingGrams));
		food.setCaloriesPer100gKcal(new BigDecimal(calories));
		food.setProteinPer100gG(new BigDecimal(protein));
		food.setCarbsPer100gG(new BigDecimal(carbs));
		food.setFatPer100gG(new BigDecimal(fat));
		return foodRepository.save(food);
	}

	private FoodEntity chickenFood() {
		return insertFood("Gà ta áp chảo", "84.55", "7.60", "10.40", "2.40", "200.00");
	}

	/**
	 * The nutritional columns of `foods` are NOT NULL, so missing nutrition can
	 * only exist through an unsynchronized schema; the nullable catalog field
	 * is standard_serving_grams and must surface as null, never as 0.
	 */
	@Test
	void getNutrition_unknownServingGrams_returnedAsNull_notZero() throws Exception {
		String token = bearerToken();
		FoodEntity food = insertFood("Tên hiếm không có khẩu phần chuẩn", "84.00", "7.60", "10.40", "2.40", null);
		mockMvc.perform(get("/api/v1/foods/%d/nutrition".formatted(food.getId())).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.standardServingGrams").value(nullValue()))
				.andExpect(jsonPath("$.standardServingLabel").value("1 phần"))
				.andExpect(jsonPath("$.caloriesPer100gKcal").value(84.0));
	}

	@Test
	void nutritionEndpoints_requireAuthentication() throws Exception {
		FoodEntity food = chickenFood();
		mockMvc.perform(get("/api/v1/foods/%d/nutrition".formatted(food.getId()))).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":200}").formatted(food.getId())))
				.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------- GET /foods/{id}/nutrition

	@Test
	void getNutrition_existingFood_returnsReferenceValuesWithUnitsAndServing() throws Exception {
		String token = bearerToken();
		FoodEntity food = insertFood("Phở gà", "84.00", "7.60", "10.40", "2.40", "500.00");
		mockMvc.perform(get("/api/v1/foods/%d/nutrition".formatted(food.getId())).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.foodId").value(food.getId().intValue()))
				.andExpect(jsonPath("$.nameVi").value("Phở gà"))
				.andExpect(jsonPath("$.nutritionReference").value("per_100g"))
				.andExpect(jsonPath("$.standardServingLabel").value("1 phần"))
				.andExpect(jsonPath("$.standardServingGrams").value(500.0))
				.andExpect(jsonPath("$.caloriesPer100gKcal").value(84.0))
				.andExpect(jsonPath("$.proteinPer100gG").value(7.6))
				.andExpect(jsonPath("$.carbsPer100gG").value(10.4))
				.andExpect(jsonPath("$.fatPer100gG").value(2.4));
	}

	@Test
	void getNutrition_nonExistentFood_returns404() throws Exception {
		String token = bearerToken();
		mockMvc.perform(get("/api/v1/foods/999999999/nutrition").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail", containsString("Món ăn không tồn tại")));
	}

	// ------------------------------------------------- POST /nutrition/calculate

	@Test
	void calculate_100g_returnsTheExactReferenceValues() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		MvcResult result = mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":100}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.foodId").value(food.getId().intValue()))
				.andExpect(jsonPath("$.foodName").value("Gà ta áp chảo"))
				.andExpect(jsonPath("$.caloriesKcal").value(84.55))
				.andExpect(jsonPath("$.proteinG").value(7.6))
				.andExpect(jsonPath("$.carbsG").value(10.4))
				.andExpect(jsonPath("$.fatG").value(2.4))
				.andReturn();
		assertThat(readField(result, "weight").decimalValue().compareTo(new BigDecimal("100"))).isEqualTo(0);
	}

	@Test
	void calculate_200g_doublesTheReferenceValues() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		MvcResult result = mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":200}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.caloriesKcal").value(169.1))
				.andExpect(jsonPath("$.proteinG").value(15.2))
				.andExpect(jsonPath("$.carbsG").value(20.8))
				.andExpect(jsonPath("$.fatG").value(4.8))
				.andReturn();
		assertThat(readField(result, "weight").decimalValue().compareTo(new BigDecimal("200"))).isEqualTo(0);
	}

	@Test
	void calculate_decimalWeight_usesHalfUpAndNoDoublePrecisionNoise() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		// 84.55 * 12.34 / 100 = 10.43347… -> 10.43; 7.60*12.34/100 = 0.93784 -> 0.94;
		// 10.40*12.34/100 = 1.28336 -> 1.28; 2.40*12.34/100 = 0.29616 -> 0.30.
		MvcResult result = mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":12.34}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.caloriesKcal").value(10.43))
				.andExpect(jsonPath("$.proteinG").value(0.94))
				.andExpect(jsonPath("$.carbsG").value(1.28))
				.andExpect(jsonPath("$.fatG").value(0.3))
				.andReturn();
		assertThat(readField(result, "weight").decimalValue().compareTo(new BigDecimal("12.34"))).isEqualTo(0);
	}

	@Test
	void calculate_veryLargeButValidWeight_isAcceptedAndCalculated() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		BigDecimal weight = new BigDecimal("999999.99");
		BigDecimal expectedCalories = food.getCaloriesPer100gKcal().multiply(weight)
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		MvcResult result = mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":%s}").formatted(food.getId(), weight))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(readField(result, "caloriesKcal").decimalValue().compareTo(expectedCalories)).isEqualTo(0);
	}

	@Test
	void calculate_weightBeyondDatabaseCapacity_isRejected() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":10000000}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]")
						.value("weight: Trọng lượng (gram) tối đa là 9.999.999,99 g và tối đa 2 chữ số thập phân."));
	}

	@Test
	void calculate_zeroWeight_isRejected() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":0}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Dữ liệu không hợp lệ"))
				.andExpect(jsonPath("$.fieldErrors[0]").value("weight: Trọng lượng (gram) phải lớn hơn 0."));
	}

	@Test
	void calculate_negativeWeight_isRejected() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":-150}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]").value("weight: Trọng lượng (gram) phải lớn hơn 0."));
	}

	@Test
	void calculate_nullWeight_isRejected() throws Exception {
		String token = bearerToken();
		FoodEntity food = chickenFood();
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content(("{\"foodId\":%d,\"weight\":null}").formatted(food.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]").value("weight: Trọng lượng (gram) là bắt buộc."));
	}

	@Test
	void calculate_invalidFoodId_isRejected() throws Exception {
		String token = bearerToken();
		chickenFood();
		// Missing database record -> 404, consistent with GET /foods/{id}/nutrition.
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content("{\"foodId\":999999999,\"weight\":200}")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail", containsString("Món ăn không tồn tại (id=999999999)")));
		// Non positive ids (missing / zero / negative) -> 400 by bean validation.
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content("{\"weight\":200}")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]").value("foodId: foodId là bắt buộc."));
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content("{\"foodId\":0,\"weight\":200}")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]").value("foodId: foodId phải là số nguyên dương."));
	}

	@Test
	void calculate_malformedBody_isRejected() throws Exception {
		String token = bearerToken();
		mockMvc.perform(post("/api/v1/nutrition/calculate").contentType("application/json")
				.content("{foodId not json")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	// ------------------------------------------- POST /nutrition/calculate-multiple

	@Test
	void calculateMultiple_returnsEachItemIndependentlyWithTotals() throws Exception {
		String token = bearerToken();
		FoodEntity chicken = chickenFood();
		FoodEntity rice = insertFood("Cơm tấm sườn", "155.00", "8.00", "19.50", "4.50", "400.00");
		MvcResult result = mockMvc.perform(post("/api/v1/nutrition/calculate-multiple").contentType("application/json")
				.content(("{\"items\":[{\"foodId\":%d,\"weight\":200},{\"foodId\":%d,\"weight\":150},{\"foodId\":%d,\"weight\":5.5}]}")
						.formatted(chicken.getId(), rice.getId(), chicken.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itemCount").value(3))
				.andExpect(jsonPath("$.items[0].foodId").value(chicken.getId().intValue()))
				.andExpect(jsonPath("$.items[0].foodName").value("Gà ta áp chảo"))
				.andExpect(jsonPath("$.items[0].caloriesKcal").value(169.1))
				.andExpect(jsonPath("$.items[1].foodId").value(rice.getId().intValue()))
				.andExpect(jsonPath("$.items[1].foodName").value("Cơm tấm sườn"))
				.andExpect(jsonPath("$.items[2].weight").value(5.5))
				.andExpect(jsonPath("$.totals.caloriesKcal").value(406.25))
				.andExpect(jsonPath("$.totals.proteinG").value(27.62))
				.andExpect(jsonPath("$.totals.carbsG").value(50.62))
				.andExpect(jsonPath("$.totals.fatG").value(11.68))
				.andReturn();
		assertThat(readField(result, "itemCount").asInt()).isEqualTo(3);
	}

	@Test
	void calculateMultiple_totalsEqualTheSumOfTheIndividualResults() throws Exception {
		String token = bearerToken();
		FoodEntity chicken = chickenFood();
		FoodEntity rice = insertFood("Cơm tấm sườn", "155.00", "8.00", "19.50", "4.50", null);
		MvcResult result = mockMvc.perform(post("/api/v1/nutrition/calculate-multiple").contentType("application/json")
				.content(("{\"items\":[{\"foodId\":%d,\"weight\":270.03},{\"foodId\":%d,\"weight\":140.53}]}")
						.formatted(chicken.getId(), rice.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		ItemSum sums = new ItemSum();
		for (JsonNode item : body.get("items")) {
			sums.weight = sums.weight.add(item.get("weight").decimalValue());
			sums.calories = sums.calories.add(item.get("caloriesKcal").decimalValue());
			sums.protein = sums.protein.add(item.get("proteinG").decimalValue());
			sums.carbs = sums.carbs.add(item.get("carbsG").decimalValue());
			sums.fat = sums.fat.add(item.get("fatG").decimalValue());
		}
		assertThat(body.get("totals").get("weight").decimalValue().compareTo(sums.weight)).isEqualTo(0);
		assertThat(body.get("totals").get("caloriesKcal").decimalValue().compareTo(sums.calories)).isEqualTo(0);
		assertThat(body.get("totals").get("proteinG").decimalValue().compareTo(sums.protein)).isEqualTo(0);
		assertThat(body.get("totals").get("carbsG").decimalValue().compareTo(sums.carbs)).isEqualTo(0);
		assertThat(body.get("totals").get("fatG").decimalValue().compareTo(sums.fat)).isEqualTo(0);
	}

	@Test
	void calculateMultiple_missingFoodId_failsTheWholeRequestWith404() throws Exception {
		String token = bearerToken();
		FoodEntity chicken = chickenFood();
		mockMvc.perform(post("/api/v1/nutrition/calculate-multiple").contentType("application/json")
				.content(("{\"items\":[{\"foodId\":%d,\"weight\":200},{\"foodId\":999999999,\"weight\":150}]}")
						.formatted(chicken.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail", containsString("Món ăn không tồn tại (id=999999999)")));
	}

	@Test
	void calculateMultiple_invalidItemValues_areRejectedWithFieldErrors() throws Exception {
		String token = bearerToken();
		FoodEntity chicken = chickenFood();
		// A zero item weight must be rejected with the offending index.
		mockMvc.perform(post("/api/v1/nutrition/calculate-multiple").contentType("application/json")
				.content(("{\"items\":[{\"foodId\":%d,\"weight\":0}]}").formatted(chicken.getId()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]").value("items[0].weight: Trọng lượng (gram) phải lớn hơn 0."));
		// An empty list is not a usable calculation request.
		mockMvc.perform(post("/api/v1/nutrition/calculate-multiple").contentType("application/json")
				.content("{\"items\":[]}")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0]")
						.value("items: items không được để trống; cần ít nhất một món ăn."));
	}

	// ------------------------------------------------------------- json helpers

	private JsonNode readField(MvcResult result, String field) throws java.io.UnsupportedEncodingException {
		return objectMapper.readTree(result.getResponse().getContentAsString()).get(field);
	}

	private static final class ItemSum {
		BigDecimal weight = BigDecimal.ZERO;
		BigDecimal calories = BigDecimal.ZERO;
		BigDecimal protein = BigDecimal.ZERO;
		BigDecimal carbs = BigDecimal.ZERO;
		BigDecimal fat = BigDecimal.ZERO;
	}
}
