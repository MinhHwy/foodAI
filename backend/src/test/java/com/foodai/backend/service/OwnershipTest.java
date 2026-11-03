package com.foodai.backend.service;

import com.foodai.backend.entity.User;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.repository.*;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnershipTest {
    @Test
    void rejectsAnonymousRequests() {
        CurrentFoodUser resolver = new CurrentFoodUser(mock(UserRepository.class));
        FoodApiException ex = assertThrows(FoodApiException.class, () -> resolver.require(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void resolvesOnlyActiveKnownUserByVerifiedPrincipalEmail() {
        UserRepository users = mock(UserRepository.class);
        User active = new User(7L);
        active.setEmail("user@example.com");
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(active));
        assertEquals(7L, new CurrentFoodUser(users).require((Principal) () -> "user@example.com").getId());
    }

    @Test
    void recognitionSessionMustBelongToUser() {
        RecognitionSessionRepository sessions = mock(RecognitionSessionRepository.class);
        RecognitionService service = new RecognitionService(sessions,
                mock(RecognitionItemRepository.class), mock(RecognitionFeedbackRepository.class),
                mock(FoodRepository.class), new NutritionCalculator());
        FoodApiException ex = assertThrows(FoodApiException.class,
                () -> service.get(100L, new User(7L)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(sessions).findByIdAndUserId(100L, 7L);
    }

    @Test
    void diaryMustBelongToUser() {
        DiaryMealRepository meals = mock(DiaryMealRepository.class);
        FoodDiaryService service = new FoodDiaryService(meals,
                mock(DiaryItemRepository.class), mock(FoodRepository.class),
                mock(RecognitionItemRepository.class), mock(UserSettingsRepository.class),
                new NutritionCalculator());
        FoodApiException ex = assertThrows(FoodApiException.class,
                () -> service.get(new User(7L), 101L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(meals).findByIdAndUserId(101L, 7L);
    }
}
