package com.foodai.backend.security;

import com.foodai.backend.config.SecurityConfig;
import com.foodai.backend.controller.HealthController;
import com.foodai.backend.controller.RecognitionController;
import com.foodai.backend.entity.User;
import com.foodai.backend.service.RecognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                HealthController.class,
                RecognitionController.class,
                SecurityProbeController.class
        },
        properties = {
                "security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "security.jwt.expiration-ms=900000",
                "app.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500"
        })
@Import({
        SecurityConfig.class,
        JwtService.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class SecurityConfigurationTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private RecognitionService recognitionService;

    private FoodAIUserDetails user;

    @BeforeEach
    void setUp() {
        User entity = mock(User.class);
        when(entity.getId()).thenReturn(42L);
        when(entity.getEmail()).thenReturn("user@example.test");
        when(entity.getPasswordHash()).thenReturn("$2a$10$test-hash");
        when(entity.getRole()).thenReturn(User.Role.USER);
        when(entity.getAccountStatus()).thenReturn(User.AccountStatus.ACTIVE);
        when(entity.getTokenVersion()).thenReturn(7);
        user = FoodAIUserDetails.from(entity);
        when(userDetailsService.loadUserById(42L)).thenReturn(user);
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("FoodAI Backend is running"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void recognitionEndpointRequiresAuthentication() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "meal.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/recognitions").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenAuthenticatesUserAndExposesServerLoadedUserId() throws Exception {
        String token = jwtService.generateToken(user);
        var claims = jwtService.parseToken(token);
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonLocked());
        assertTrue(jwtService.isValidFor(claims, user));

        var result = mockMvc.perform(get("/api/v1/test/current-user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        verify(userDetailsService).loadUserById(42L);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, 1);
        String token = shortLivedJwtService.generateToken(user);
        Thread.sleep(10);

        mockMvc.perform(get("/api/v1/test/current-user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserWithoutAdminRoleReceivesForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void corsAllowsConfiguredLiveServerOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/test/current-user")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5500"));
    }

    @Test
    void passwordEncoderIsBcrypt() {
        String rawPassword = "test-password-not-for-production";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        org.junit.jupiter.api.Assertions.assertNotEquals(rawPassword, encodedPassword);
        org.junit.jupiter.api.Assertions.assertTrue(encodedPassword.startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }
}

@RestController
class SecurityProbeController {

    @GetMapping("/api/v1/test/current-user")
    Map<String, Long> currentUser(@org.springframework.security.core.annotation.AuthenticationPrincipal
                                  FoodAIUserDetails user) {
        return Map.of("userId", user.getUserId());
    }

    @GetMapping("/api/v1/audit-logs/test")
    Map<String, String> adminOnlyProbe() {
        return Map.of("status", "OK");
    }
}
