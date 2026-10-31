package com.foodai.backend;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.foodai.backend.profile.entity.UserEntity;
import com.foodai.backend.profile.repository.UserRepository;

@SpringBootTest(properties = "foodai.jwt.secret=" + TestSupport.JWT_SECRET)
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	private static String registerBody(String fullName, String email, String password) {
		return """
				{"fullName":"%s","email":"%s","password":"%s"}""".formatted(fullName, email, password);
	}

	private String register(String fullName, String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody(fullName, email, password))).andReturn().getResponse().getContentAsString();
	}

	@Test
	void registerReturns201WithUserDataAndNoSecrets() throws Exception {
		String email = TestSupport.uniqueEmail();
		String body = mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody("Minh Anh", email, "Passw0rd123")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(body).doesNotContain("password").doesNotContain("passwordHash")
				.contains("fullName");
	}

	@Test
	void registerStoresBcryptHashNotPlaintext() throws Exception {
		String email = TestSupport.uniqueEmail();
		register("Minh Anh", email, "Passw0rd123");
		UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(user.getPasswordHash()).startsWith("$2")
				.isNotEqualTo("Passw0rd123");
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		String email = TestSupport.uniqueEmail();
		register("Minh Anh", email, "Passw0rd123");
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody("Minh Anh 2", email, "Passw0rd123"))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void registerRejectsInvalidInput() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody("Minh Anh", "khong-phai-email", "Passw0rd123")))
				.andExpect(status().isBadRequest());
		// password shorter than 8 characters
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody("Minh Anh", TestSupport.uniqueEmail(), "short1")))
				.andExpect(status().isBadRequest());
		// password without any digit must be rejected (8+ chars, letters + digits)
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody("Minh Anh", TestSupport.uniqueEmail(), "abcdefgh")))
				.andExpect(status().isBadRequest());
		// missing fullName
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content("{\"email\":\"" + TestSupport.uniqueEmail() + "\",\"password\":\"Passw0rd123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginReturnsBearerToken() throws Exception {
		String email = TestSupport.uniqueEmail();
		register("Minh Anh", email, "Passw0rd123");
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value(not(emptyOrNullString())))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(greaterThan(0)))
				.andExpect(jsonPath("$.user.email").value(email));
	}

	@Test
	void registerNormalizesEmailToLowerCase() throws Exception {
		// registered with mixed case; stored canonically lower-case
		register("Minh Anh", "MiNh.AnH-7138@FoOdAi.EXAMPLE.COM", "Passw0rd123");
		UserEntity user = userRepository.findByEmailIgnoreCase("minh.anh-7138@foodai.example.com").orElseThrow();
		org.assertj.core.api.Assertions.assertThat(user.getEmail()).isEqualTo("minh.anh-7138@foodai.example.com");
		// case-variant duplicate must be rejected as a duplicate
		mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(registerBody("Minh Anh 2", "MINH.ANH-7138@foodai.example.com", "Passw0rd123")))
				.andExpect(status().isConflict());
		// login works regardless of the case used by the client
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"MINH.ANH-7138@FOODAI.EXAMPLE.COM\",\"password\":\"Passw0rd123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("minh.anh-7138@foodai.example.com"));
	}

	@Test
	void loginRejectsWrongPassword() throws Exception {
		String email = TestSupport.uniqueEmail();
		register("Minh Anh", email, "Passw0rd123");
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"WrongPass1\"}".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không chính xác."));
	}

	@Test
	void loginRejectsUnknownEmail() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"" + TestSupport.uniqueEmail() + "\",\"password\":\"Passw0rd123\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginRejectsSuspendedAndDeletedAccounts() throws Exception {
		String email = TestSupport.uniqueEmail();
		register("Minh Anh", email, "Passw0rd123");
		UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		user.setAccountStatus(UserEntity.AccountStatus.SUSPENDED);
		userRepository.save(user);
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
		user.setAccountStatus(UserEntity.AccountStatus.DELETED);
		userRepository.save(user);
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"Passw0rd123\"}".formatted(email)))
				.andExpect(status().isForbidden());
	}
}
